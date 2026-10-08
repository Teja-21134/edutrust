package com.edutrust.retrieval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchServiceRrfTest {

    @Test
    void calculatesStandardRrfContribution() {
        assertThat(SearchService.rrfContribution(1, 60)).isEqualTo(1.0 / 61.0);
        assertThat(SearchService.rrfContribution(3, 60)).isEqualTo(1.0 / 63.0);
        assertThat(SearchService.rrfContribution(1, 60) + SearchService.rrfContribution(3, 60))
                .isEqualTo(1.0 / 61.0 + 1.0 / 63.0);
    }
}
