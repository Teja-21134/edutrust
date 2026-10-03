package com.edutrust.api;

import com.edutrust.generation.AnswerServiceUnavailableException;
import com.edutrust.generation.AskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/ask")
public class AskController {

    private static final String UNAVAILABLE_MESSAGE =
            "The answer service is not available. Please try again.";

    private final AskService askService;

    public AskController(AskService askService) {
        this.askService = askService;
    }

    @PostMapping
    public AskService.AskResult ask(@RequestBody AskRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question must not be empty");
        }
        return askService.ask(request.question());
    }

    @ExceptionHandler(AnswerServiceUnavailableException.class)
    public ResponseEntity<Map<String, String>> answerServiceUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", UNAVAILABLE_MESSAGE));
    }

    public record AskRequest(String question) {
    }
}
