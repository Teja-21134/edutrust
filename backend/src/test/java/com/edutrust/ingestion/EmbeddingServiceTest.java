package com.edutrust.ingestion;

import com.edutrust.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EmbeddingServiceTest extends IntegrationTestBase {

    @Autowired
    private EmbeddingService embeddingService;

    @Test
    void creates384DimensionalVectorsInBatches() {
        List<float[]> vectors = embeddingService.embedAll(List.of(
                "Students must maintain attendance in every course.",
                "Every student is required to attend classes regularly."));

        assertThat(vectors).hasSize(2);
        assertThat(vectors).allSatisfy(vector -> assertThat(vector).hasSize(384));
    }

    @Test
    void similarSentencesAreCloserThanAnUnrelatedSentence() {
        List<float[]> vectors = embeddingService.embedAll(List.of(
                "Students must maintain attendance in every course.",
                "Every student is required to attend classes regularly.",
                "The examination hall prohibits mobile phones."));

        assertThat(cosineSimilarity(vectors.get(0), vectors.get(1)))
                .isGreaterThan(cosineSimilarity(vectors.get(0), vectors.get(2)));
    }

    private double cosineSimilarity(float[] first, float[] second) {
        double dot = 0;
        double firstMagnitude = 0;
        double secondMagnitude = 0;
        for (int index = 0; index < first.length; index++) {
            dot += first[index] * second[index];
            firstMagnitude += first[index] * first[index];
            secondMagnitude += second[index] * second[index];
        }
        return dot / (Math.sqrt(firstMagnitude) * Math.sqrt(secondMagnitude));
    }
}
