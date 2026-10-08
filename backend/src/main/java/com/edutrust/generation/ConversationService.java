package com.edutrust.generation;

import com.edutrust.database.AppUser;
import com.edutrust.database.Conversation;
import com.edutrust.database.ConversationRepository;
import com.edutrust.database.Message;
import com.edutrust.database.MessageRepository;
import com.edutrust.database.MessageRole;
import com.edutrust.database.UserRepository;
import com.edutrust.security.AuthenticatedUser;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ConversationService {

    private static final String DEFAULT_TITLE = "New conversation";
    private static final String CHAT_PIPELINE_VERSION = "v3";

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AskService askService;

    public ConversationService(UserRepository userRepository,
                               ConversationRepository conversationRepository,
                               MessageRepository messageRepository,
                               AskService askService) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.askService = askService;
    }

    @Transactional
    public ConversationView create(AuthenticatedUser authenticatedUser, String requestedTitle) {
        AppUser owner = currentUser(authenticatedUser);
        String title = requestedTitle == null || requestedTitle.isBlank()
                ? DEFAULT_TITLE : requestedTitle.trim();
        Conversation conversation = conversationRepository.save(
                new Conversation(UUID.randomUUID(), owner, title));
        return toView(conversation);
    }

    @Transactional
    public List<ConversationSummary> list(AuthenticatedUser authenticatedUser) {
        AppUser owner = currentUser(authenticatedUser);
        return conversationRepository.findByOwner_IdOrderByUpdatedAtDesc(owner.getId()).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public ConversationView get(AuthenticatedUser authenticatedUser, UUID conversationId) {
        return toView(ownedConversation(authenticatedUser, conversationId));
    }

    @Transactional
    public void delete(AuthenticatedUser authenticatedUser, UUID conversationId) {
        conversationRepository.delete(ownedConversation(authenticatedUser, conversationId));
    }

    @Transactional
    public MessageResponse addMessage(AuthenticatedUser authenticatedUser, UUID conversationId, String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question must not be empty");
        }
        Conversation conversation = ownedConversation(authenticatedUser, conversationId);
        String trimmedQuestion = question.trim();
        if (DEFAULT_TITLE.equals(conversation.getTitle())) {
            conversation.setTitle(trimmedQuestion.length() <= 255
                    ? trimmedQuestion : trimmedQuestion.substring(0, 252) + "...");
        }

        Message userMessage = new Message(UUID.randomUUID(), MessageRole.USER, trimmedQuestion);
        conversation.addMessage(userMessage);
        messageRepository.save(userMessage);

        AskService.AskResult result = askService.ask(trimmedQuestion, CHAT_PIPELINE_VERSION);
        Message assistantMessage = new Message(UUID.randomUUID(), MessageRole.ASSISTANT, result.answer());
        assistantMessage.setAssistantMetadata(result.sources().stream()
                        .map(source -> {
                            Map<String, Object> metadata = new LinkedHashMap<>();
                            metadata.put("documentTitle", source.documentTitle());
                            metadata.put("pageNumber", source.pageNumber());
                            metadata.put("version", source.version());
                            return metadata;
                        }).toList(),
                result.faithfulnessStatus(), result.faithfulnessScore(), result.conflictDetected(),
                result.conflictResolution(), result.selectedDocument(), result.selectedVersion());
        conversation.addMessage(assistantMessage);
        messageRepository.save(assistantMessage);
        conversationRepository.save(conversation);

        return new MessageResponse(conversationId, toMessageView(userMessage), toMessageView(assistantMessage), result);
    }

    private AppUser currentUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.email() == null) {
            throw new UnauthorizedConversationAccessException();
        }
        return userRepository.findByEmail(authenticatedUser.email())
                .orElseThrow(UnauthorizedConversationAccessException::new);
    }

    private Conversation ownedConversation(AuthenticatedUser authenticatedUser, UUID conversationId) {
        AppUser owner = currentUser(authenticatedUser);
        return conversationRepository.findByIdAndOwner_Id(conversationId, owner.getId())
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    private ConversationSummary toSummary(Conversation conversation) {
        return new ConversationSummary(conversation.getId(), conversation.getTitle(),
                conversation.getCreatedAt(), conversation.getUpdatedAt());
    }

    private ConversationView toView(Conversation conversation) {
        return new ConversationView(conversation.getId(), conversation.getTitle(),
                conversation.getCreatedAt(), conversation.getUpdatedAt(),
                conversation.getMessages().stream().map(this::toMessageView).toList());
    }

    private MessageView toMessageView(Message message) {
        return new MessageView(message.getId(), message.getRole(), message.getContent(),
                message.getSources(), message.getFaithfulnessStatus(), message.getFaithfulnessScore(),
                message.getConflictDetected(), message.getConflictResolution(),
                message.getSelectedDocument(), message.getSelectedVersion(), message.getCreatedAt());
    }

    public record ConversationSummary(UUID id, String title,
                                      java.time.LocalDateTime createdAt,
                                      java.time.LocalDateTime updatedAt) {}

    public record ConversationView(UUID id, String title,
                                   java.time.LocalDateTime createdAt,
                                   java.time.LocalDateTime updatedAt,
                                   List<MessageView> messages) {}

    public record MessageView(UUID id, MessageRole role, String content,
                              List<Map<String, Object>> sources,
                              String faithfulnessStatus, Double faithfulnessScore,
                              Boolean conflictDetected, String conflictResolution,
                              String selectedDocument, String selectedVersion,
                              java.time.LocalDateTime createdAt) {}

    public record MessageResponse(UUID conversationId, MessageView userMessage,
                                  MessageView assistantMessage, AskService.AskResult answer) {}

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ConversationNotFoundException extends RuntimeException {
        public ConversationNotFoundException(UUID conversationId) {
            super("Conversation not found: " + conversationId);
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class UnauthorizedConversationAccessException extends RuntimeException {
        public UnauthorizedConversationAccessException() {
            super("Authenticated user was not found");
        }
    }
}
