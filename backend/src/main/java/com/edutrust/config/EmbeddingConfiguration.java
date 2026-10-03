package com.edutrust.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15.BgeSmallEnV15EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Creates the local embedding model once and shares it across ingestion requests. */
@Configuration
public class EmbeddingConfiguration {

    @Bean
    EmbeddingModel embeddingModel(@Value("${edutrust.embedding.model}") String modelName) {
        if (!"bge-small-en-v1.5".equals(modelName)) {
            throw new IllegalArgumentException("Unsupported embedding model: " + modelName);
        }
        return new BgeSmallEnV15EmbeddingModel();
    }
}
