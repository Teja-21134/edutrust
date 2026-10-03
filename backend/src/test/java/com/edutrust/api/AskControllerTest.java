package com.edutrust.api;

import com.edutrust.generation.AnswerServiceUnavailableException;
import com.edutrust.generation.AskService;
import com.edutrust.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AskController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(AskControllerTest.StubConfig.class)
class AskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returns503WhenAnswerServiceIsUnavailable() throws Exception {
        mockMvc.perform(post("/api/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is the attendance requirement?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("The answer service is not available. Please try again."));
    }

    @TestConfiguration
    static class StubConfig {

        @Bean
        AskService askService() {
            return new AskService(null, null) {
                @Override
                public AskResult ask(String question) {
                    throw new AnswerServiceUnavailableException(new RuntimeException("Ollama unavailable"));
                }
            };
        }
    }
}
