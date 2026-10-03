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
}
