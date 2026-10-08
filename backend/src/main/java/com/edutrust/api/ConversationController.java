package com.edutrust.api;

import com.edutrust.generation.AnswerServiceUnavailableException;
import com.edutrust.generation.ConversationService;
import com.edutrust.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ConversationService.ConversationView create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody(required = false) CreateConversationRequest request) {
        return conversationService.create(user, request == null ? null : request.title());
    }

    @GetMapping
    public List<ConversationService.ConversationSummary> list(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return conversationService.list(user);
    }

    @GetMapping("/{id}")
    public ConversationService.ConversationView get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        return conversationService.get(user, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        conversationService.delete(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/messages")
    public ConversationService.MessageResponse addMessage(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestBody MessageRequest request) {
        return conversationService.addMessage(user, id, request == null ? null : request.question());
    }

    @ExceptionHandler(AnswerServiceUnavailableException.class)
    public ResponseEntity<Map<String, String>> answerServiceUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "The answer service is not available. Please try again."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    public record CreateConversationRequest(String title) {}

    public record MessageRequest(String question) {}
}
