package com.edutrust.generation;

import com.edutrust.retrieval.SearchService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AskServiceTest {

    @Test
    void notFoundAnswerReturnsNoSourcesAndAnsweredFalse() {
        SearchService searchService = new SearchService(null, null, 1) {
            @Override
            public List<SearchHit> search(String question) {
                return List.of();
            }
        };
        AnswerService answerService = new AnswerService(messages -> null, "") {
            @Override
            public String answer(String question, List<SearchService.SearchHit> hits) {
                return AnswerService.NOT_FOUND_ANSWER;
            }
        };

        AskService.AskResult result = new AskService(searchService, answerService).ask("unknown question");

        assertThat(result.answer()).isEqualTo(AnswerService.NOT_FOUND_ANSWER);
        assertThat(result.answered()).isFalse();
        assertThat(result.sources()).isEmpty();
    }

    @Test
    void unresolvedConflictWithoutSelectedEvidenceUsesNotFoundAnswer() {
        SearchService searchService = new SearchService(null, null, 1) {
            @Override
            public List<SearchHit> searchHybrid(String question) {
                return List.of(new SearchHit(
                        java.util.UUID.randomUUID(), "Conflicting values are documented.", 1,
                        java.util.UUID.randomUUID(), "Regulations", "All", "Regulations",
                        "2026-27", "v3", null, 0.9));
            }
        };
        AnswerService answerService = new AnswerService(messages -> null, "") {
            @Override
            public String answer(String question, List<SearchService.SearchHit> hits,
                                 ConflictResolutionService.Resolution resolution) {
                return "should not be called";
            }
        };
        ConflictResolutionService conflictResolver = new ConflictResolutionService(
                new com.edutrust.config.InstitutionProperties()) {
            @Override
            public Resolution resolve(String question, List<SearchService.SearchHit> hits) {
                return new Resolution(List.of(), true, "unresolved_conflict", null, null,
                        "internal conflict metadata");
            }
        };

        AskService.AskResult result = new AskService(
                searchService, answerService, conflictResolver, new FaithfulnessVerifier(0.55))
                .ask("unknown fact", "v3");

        assertThat(result.answer()).isEqualTo(AnswerService.NOT_FOUND_ANSWER);
        assertThat(result.conflictDetected()).isTrue();
        assertThat(result.conflictResolution()).isEqualTo("unresolved_conflict");
        assertThat(result.answered()).isFalse();
    }
}
