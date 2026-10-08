package com.edutrust.generation;

import com.edutrust.retrieval.SearchService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Builds the evidence prompt and delegates answer generation to Ollama. */
@Service
public class AnswerService {

    private static final Logger log = LoggerFactory.getLogger(AnswerService.class);

    public static final String NOT_FOUND_ANSWER =
            "I could not find this information in the available documents.";

    private final ChatLanguageModel chatModel;
    private final String promptTemplate;

    @Autowired
    public AnswerService(ChatLanguageModel chatModel) {
        this(chatModel, loadPromptTemplate());
    }

    public AnswerService(ChatLanguageModel chatModel, String promptTemplate) {
        this.chatModel = chatModel;
        this.promptTemplate = promptTemplate;
    }

    public String answer(String question, List<SearchService.SearchHit> hits) {
        return answer(question, hits, null);
    }

    /** Generates an answer from the selected document evidence only. */
    public String answer(String question, List<SearchService.SearchHit> hits,
                         ConflictResolutionService.Resolution resolution) {
        String evidence = buildEvidence(hits);
        String prompt = promptTemplate
                .replace("{{question}}", question)
                .replace("{{evidence}}", evidence);
        String userInstruction = "Answer only from the supplied document evidence. "
                + "Do not use retrieval, ranking, conflict-resolution, document-selection, version, score, "
                + "or pipeline metadata as facts, and do not mention those internal details. "
                + "If the evidence does not directly contain the requested information, return exactly: "
                + NOT_FOUND_ANSWER;
        log.debug("Full prompt sent to model:\nSystem:\n{}\nUser:\n{}", prompt, userInstruction);
        String answer = chatModel.generate(List.of(
                SystemMessage.from(prompt),
                UserMessage.from(userInstruction))).content().text().trim();
        if (resolution != null && NOT_FOUND_ANSWER.equalsIgnoreCase(answer)) {
            String evidenceFallback = evidenceFallback(question, hits);
            if (evidenceFallback != null) {
                log.info("V3 generation returned not-found with supplied evidence; using evidence sentence fallback");
                return evidenceFallback;
            }
        }
        return answer;
    }

    private String buildEvidence(List<SearchService.SearchHit> hits) {
        List<String> evidence = new ArrayList<>();
        for (int index = 0; index < hits.size(); index++) {
            SearchService.SearchHit hit = hits.get(index);
            evidence.add("[%d] %s, page %d\n%s".formatted(
                    index + 1, hit.documentTitle(), hit.pageNumber(), hit.text()));
        }
        return String.join("\n\n", evidence);
    }

    private String evidenceFallback(String question, List<SearchService.SearchHit> hits) {
        Set<String> questionTerms = factualTerms(question);
        for (SearchService.SearchHit hit : hits) {
            for (String sentence : hit.text().split("(?<=[.!?])\\s+")) {
                String normalized = sentence.trim();
                if (!normalized.isBlank() && normalized.matches(".*\\d.*")
                        && questionTerms.stream().anyMatch(normalized.toLowerCase(Locale.ROOT)::contains)) {
                    return normalized;
                }
            }
        }
        return null;
    }

    private Set<String> factualTerms(String question) {
        Set<String> terms = new HashSet<>();
        Arrays.stream((question == null ? "" : question.toLowerCase(Locale.ROOT)).split("\\W+"))
                .filter(term -> term.length() > 2)
                .forEach(term -> terms.add(term.endsWith("s") ? term.substring(0, term.length() - 1) : term));
        return terms;
    }

    private static String loadPromptTemplate() {
        try (var inputStream = new ClassPathResource("prompts/answer.txt").getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load answer prompt template", exception);
        }
    }
}
