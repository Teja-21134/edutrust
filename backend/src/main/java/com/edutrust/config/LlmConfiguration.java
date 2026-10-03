package com.edutrust.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/** Configures one local Ollama chat model for answer generation. */
@Configuration
public class LlmConfiguration {

    @Bean
    ChatLanguageModel ollamaChatModel(
            @Value("${edutrust.llm.base-url}") String baseUrl,
            @Value("${edutrust.llm.model}") String model,
            @Value("${edutrust.llm.temperature}") double temperature,
            @Value("${edutrust.llm.timeout-seconds}") long timeoutSeconds) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }
}
