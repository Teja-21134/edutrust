package com.edutrust.ingestion;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Embeds text locally in bounded batches using the singleton BGE-small model. */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingModel embeddingModel;
    private final int batchSize;

    public EmbeddingService(
            EmbeddingModel embeddingModel,
            @Value("${edutrust.embedding.batch-size}") int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Embedding batch size must be positive");
        }
        this.embeddingModel = embeddingModel;
        this.batchSize = batchSize;
    }

    public List<float[]> embedAll(List<String> texts) {
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += batchSize) {
            int end = Math.min(start + batchSize, texts.size());
            List<TextSegment> segments = texts.subList(start, end).stream()
                    .map(TextSegment::from)
                    .toList();
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddings.forEach(embedding -> vectors.add(embedding.vector()));
        }
        List<float[]> result = List.copyOf(vectors);
        log.info("Embedded texts={} vectors={} dimensions={}",
                texts.size(), result.size(), result.isEmpty() ? 0 : result.getFirst().length);
        return result;
    }

    public float[] embed(String text) {
        return embedAll(List.of(text)).getFirst();
    }
}
