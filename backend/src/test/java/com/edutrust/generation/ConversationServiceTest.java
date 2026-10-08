package com.edutrust.generation;

import com.edutrust.database.AppUser;
import com.edutrust.database.Conversation;
import com.edutrust.database.ConversationRepository;
import com.edutrust.database.Message;
import com.edutrust.database.MessageRepository;
import com.edutrust.database.MessageRole;
import com.edutrust.database.UserRepository;
import com.edutrust.database.UserRole;
import com.edutrust.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class ConversationServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private final MessageRepository messageRepository = mock(MessageRepository.class);
    private final StubAskService askService = new StubAskService();
    private final ConversationService service = new ConversationService(
            userRepository, conversationRepository, messageRepository, askService);

    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final AppUser user = new AppUser(userId, "Student", "student@example.com", "hash", UserRole.STUDENT);
    private final AppUser otherUser = new AppUser(otherUserId, "Other", "other@example.com", "hash", UserRole.STUDENT);
    private final AuthenticatedUser authenticatedUser =
            new AuthenticatedUser("Student", "student@example.com", UserRole.STUDENT);
    private final AuthenticatedUser otherAuthenticatedUser =
            new AuthenticatedUser("Other", "other@example.com", UserRole.STUDENT);

    @BeforeEach
    void configureUserLookup() {
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsConversationForAuthenticatedUser() {
        ConversationService.ConversationView result = service.create(authenticatedUser, null);

        assertThat(result.title()).isEqualTo("New conversation");
        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner()).isSameAs(user);
    }

    @Test
    void retrievesOnlyOwnConversations() {
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = new Conversation(conversationId, user, "Attendance");
        when(conversationRepository.findByIdAndOwner_Id(conversationId, userId))
                .thenReturn(Optional.of(conversation));

        assertThat(service.get(authenticatedUser, conversationId).id()).isEqualTo(conversationId);

        when(conversationRepository.findByIdAndOwner_Id(conversationId, otherUserId))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(otherAuthenticatedUser, conversationId))
                .isInstanceOf(ConversationService.ConversationNotFoundException.class);
    }

    @Test
    void deletesOnlyOwnConversation() {
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = new Conversation(conversationId, user, "Attendance");
        when(conversationRepository.findByIdAndOwner_Id(conversationId, userId))
                .thenReturn(Optional.of(conversation));

        service.delete(authenticatedUser, conversationId);
        verify(conversationRepository).delete(conversation);

        when(conversationRepository.findByIdAndOwner_Id(conversationId, otherUserId))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(otherAuthenticatedUser, conversationId))
                .isInstanceOf(ConversationService.ConversationNotFoundException.class);
        verify(conversationRepository, org.mockito.Mockito.times(1)).delete(conversation);
    }

    @Test
    void persistsUserAndAssistantMessagesWithAskMetadata() {
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = new Conversation(conversationId, user, "New conversation");
        when(conversationRepository.findByIdAndOwner_Id(conversationId, userId))
                .thenReturn(Optional.of(conversation));
        AskService.AskResult answer = new AskService.AskResult(
                "Up to 5%.", true,
                List.of(new AskService.Source("Academic Regulations 2026", 3, "v3")),
                42, "VERIFIED", 0.98, true, "override_clause", "v3",
                "Academic Regulations 2026");
        askService.answer = answer;

        ConversationService.MessageResponse response = service.addMessage(
                authenticatedUser, conversationId, "How much credit?");

        assertThat(response.answer()).isEqualTo(answer);
        assertThat(response.userMessage().role()).isEqualTo(MessageRole.USER);
        assertThat(response.assistantMessage().role()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(response.userMessage().createdAt()).isNotNull();
        assertThat(response.assistantMessage().createdAt()).isNotNull();
        assertThat(response.assistantMessage().sources()).containsExactly(Map.of(
                "documentTitle", "Academic Regulations 2026",
                "pageNumber", 3,
                "version", "v3"));
        assertThat(response.assistantMessage().faithfulnessStatus()).isEqualTo("VERIFIED");
        assertThat(response.assistantMessage().faithfulnessScore()).isEqualTo(0.98);
        assertThat(response.assistantMessage().conflictDetected()).isTrue();
        assertThat(response.assistantMessage().conflictResolution()).isEqualTo("override_clause");
        assertThat(response.assistantMessage().selectedDocument()).isEqualTo("Academic Regulations 2026");
        assertThat(response.assistantMessage().selectedVersion()).isEqualTo("v3");
        assertThat(conversation.getMessages()).extracting(Message::getRole)
                .containsExactly(MessageRole.USER, MessageRole.ASSISTANT);
        assertThat(askService.questions).containsExactly("How much credit?");
        assertThat(askService.pipelineVersions).containsExactly("v3");
        verify(messageRepository, org.mockito.Mockito.times(2)).save(any(Message.class));
    }

    @Test
    void messageApiServiceRejectsConversationOwnedByAnotherUser() {
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findByIdAndOwner_Id(conversationId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addMessage(authenticatedUser, conversationId, "Question"))
                .isInstanceOf(ConversationService.ConversationNotFoundException.class);
        assertThat(askService.questions).isEmpty();
        verify(messageRepository, never()).save(any(Message.class));
    }

    private static class StubAskService extends AskService {
        private AskResult answer;
        private final java.util.ArrayList<String> questions = new java.util.ArrayList<>();
        private final java.util.ArrayList<String> pipelineVersions = new java.util.ArrayList<>();

        private StubAskService() {
            super(null, null);
        }

        @Override
        public AskResult ask(String question, String pipelineVersion) {
            questions.add(question);
            pipelineVersions.add(pipelineVersion);
            return answer;
        }
    }
}
