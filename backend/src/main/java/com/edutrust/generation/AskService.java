package com.edutrust.generation;

import com.edutrust.retrieval.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ConflictResolutionService conflictResolutionService;
    private final FaithfulnessVerifier faithfulnessVerifier;

    public AskService(SearchService searchService, AnswerService answerService) {
        this(searchService, answerService, null, null);
    }

    @Autowired
    public AskService(SearchService searchService, AnswerService answerService,
                      ConflictResolutionService conflictResolutionService,
                      FaithfulnessVerifier faithfulnessVerifier) {
        this.searchService = searchService;
        this.answerService = answerService;
        this.conflictResolutionService = conflictResolutionService;
        this.faithfulnessVerifier = faithfulnessVerifier;
    }

    public AskResult ask(String question) {
        return ask(question, "v1");
    }

    public AskResult ask(String question, String pipelineVersion) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question must not be empty");
        }
        if (pipelineVersion == null || (!pipelineVersion.equalsIgnoreCase("v1")
                && !pipelineVersion.equalsIgnoreCase("v2") && !pipelineVersion.equalsIgnoreCase("v3"))) {
            throw new IllegalArgumentException("Unsupported retrieval version: " + pipelineVersion);
        }

        Instant requestStarted = Instant.now();
        Instant retrievalStarted = Instant.now();
        boolean v3 = pipelineVersion.equalsIgnoreCase("v3");
        List<SearchService.SearchHit> hits = pipelineVersion.equalsIgnoreCase("v1")
                ? searchService.search(question) : searchService.searchHybrid(question);
        ConflictResolutionService.Resolution resolution = v3
                ? conflictResolutionService.resolve(question, hits)
                : new ConflictResolutionService.Resolution(hits, false, "no_conflict", null, null, "");
        List<SearchService.SearchHit> evidence = resolution.selectedEvidence();
        if (v3 && resolution.conflictDetected() && "override_clause".equals(resolution.resolution())
                && evidence.isEmpty() && resolution.selectedDocument() != null) {
            evidence = hits.stream()
                    .filter(hit -> resolution.selectedDocument().equals(hit.documentTitle())
                            && (resolution.selectedVersion() == null
                            || resolution.selectedVersion().equals(hit.version())))
                    .toList();
            log.info("V3 restored override evidence selectedDocument={} selectedVersion={} chunks={}",
                    resolution.selectedDocument(), resolution.selectedVersion(), evidence.size());
        }
        long retrievalMs = Duration.between(retrievalStarted, Instant.now()).toMillis();
        String retrievedSummary = hits.stream()
                .map(hit -> "%s page=%d score=%.4f".formatted(
                        hit.documentTitle(), hit.pageNumber(), hit.score()))
                .toList()
                .toString();

        Instant generationStarted = Instant.now();
        String answer;
        try {
            if (v3 && evidence.isEmpty()) {
                answer = AnswerService.NOT_FOUND_ANSWER;
            } else {
                answer = v3
                        ? answerService.answer(question, evidence, resolution)
                        : answerService.answer(question, evidence);
            }
        } catch (RuntimeException exception) {
            throw new AnswerServiceUnavailableException(exception);
        }
        long generationMs = Duration.between(generationStarted, Instant.now()).toMillis();
        FaithfulnessVerifier.Verification verification = v3
                ? faithfulnessVerifier.verify(answer, evidence)
                : new FaithfulnessVerifier.Verification("VERIFIED", 1.0, "V1/V2 response");
        if (v3 && resolution.conflictDetected() && evidence.isEmpty()) {
            verification = new FaithfulnessVerifier.Verification("UNVERIFIED", 0.0, resolution.explanation());
        }
        if (v3 && "UNVERIFIED".equals(verification.status()) && !resolution.conflictDetected()) {
            answer = "I could not verify this information from the available documents.";
        }
        boolean answered = !AnswerService.NOT_FOUND_ANSWER.equals(answer)
                && (!v3 || "VERIFIED".equals(verification.status()));
        List<Source> sources = answered
                ? evidence.stream()
                .map(hit -> new Source(hit.documentTitle(), hit.pageNumber(), hit.version()))
                .distinct()
                .toList()
                : List.of();
        long timeMs = Duration.between(requestStarted, Instant.now()).toMillis();

        log.info("Ask version={} question={} retrievedChunks={} retrievalMs={} generationMs={} answer={} timeMs={}",
                pipelineVersion, question, retrievedSummary, retrievalMs, generationMs, answer, timeMs);
        return new AskResult(answer, answered, sources, timeMs,
                v3 ? verification.status() : "VERIFIED", v3 ? verification.score() : 1.0,
                v3 && resolution.conflictDetected(), v3 ? resolution.resolution() : "no_conflict",
                v3 ? resolution.selectedVersion() : null, v3 ? resolution.selectedDocument() : null);
    }

    public record Source(String documentTitle, int pageNumber, String version) {
    }

    public record AskResult(String answer, boolean answered, List<Source> sources, long timeMs,
                            String faithfulnessStatus, double faithfulnessScore,
                            boolean conflictDetected, String conflictResolution,
                            String selectedVersion, String selectedDocument) {
        public AskResult(String answer, boolean answered, List<Source> sources, long timeMs) {
            this(answer, answered, sources, timeMs, "VERIFIED", 1.0,
                    false, "no_conflict", null, null);
        }
    }
}
