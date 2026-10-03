package com.edutrust.generation;

import com.edutrust.retrieval.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Coordinates vector retrieval and basic evidence-grounded answer generation. */
@Service
public class AskService {

    private static final Logger log = LoggerFactory.getLogger(AskService.class);

    private final SearchService searchService;
    private final AnswerService answerService;

    public AskService(SearchService searchService, AnswerService answerService) {
        this.searchService = searchService;
        this.answerService = answerService;
    }

    public AskResult ask(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question must not be empty");
        }

        Instant requestStarted = Instant.now();
        Instant retrievalStarted = Instant.now();
        List<SearchService.SearchHit> hits = searchService.search(question);
        long retrievalMs = Duration.between(retrievalStarted, Instant.now()).toMillis();
        String retrievedSummary = hits.stream()
                .map(hit -> "%s page=%d score=%.4f".formatted(
                        hit.documentTitle(), hit.pageNumber(), hit.score()))
                .toList()
                .toString();

        Instant generationStarted = Instant.now();
        String answer;
        try {
            answer = answerService.answer(question, hits);
        } catch (RuntimeException exception) {
            throw new AnswerServiceUnavailableException(exception);
        }
        long generationMs = Duration.between(generationStarted, Instant.now()).toMillis();
        boolean answered = !AnswerService.NOT_FOUND_ANSWER.equals(answer);
        List<Source> sources = answered
                ? hits.stream()
                .map(hit -> new Source(hit.documentTitle(), hit.pageNumber(), hit.version()))
                .distinct()
                .toList()
                : List.of();
        long timeMs = Duration.between(requestStarted, Instant.now()).toMillis();

        log.info("Ask question={} retrievedChunks={} retrievalMs={} generationMs={} answer={} timeMs={}",
                question, retrievedSummary, retrievalMs, generationMs, answer, timeMs);
        return new AskResult(answer, answered, sources, timeMs);
    }

    public record Source(String documentTitle, int pageNumber, String version) {
    }

    public record AskResult(String answer, boolean answered, List<Source> sources, long timeMs) {
    }
}
