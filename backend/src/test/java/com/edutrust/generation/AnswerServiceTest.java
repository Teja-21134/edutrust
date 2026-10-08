package com.edutrust.generation;

import com.edutrust.retrieval.SearchService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerServiceTest {

    @Test
    void buildsFairEvidencePromptWithNumberedDocumentLabels() {
        List<ChatMessage> sentMessages = new ArrayList<>();
        ChatLanguageModel stubModel = messages -> {
            sentMessages.addAll(messages);
            return Response.from(AiMessage.from("80% attendance is required."));
        };
        AnswerService answerService = new AnswerService(stubModel);
        SearchService.SearchHit hit = new SearchService.SearchHit(
                UUID.randomUUID(), "Students must maintain 80% attendance.", 3,
                UUID.randomUUID(), "Academic Regulations 2026", "All", "Regulations",
                "2026-27", "v3", LocalDate.of(2026, 6, 15), 0.9123);

        String answer = answerService.answer("What attendance is required?", List.of(hit));

        assertThat(answer).isEqualTo("80% attendance is required.");
        assertThat(sentMessages).first().isInstanceOf(SystemMessage.class);
        String prompt = ((SystemMessage) sentMessages.getFirst()).text();
        assertThat(prompt)
                .contains("[1] Academic Regulations 2026, page 3")
                .contains("Students must maintain 80% attendance.")
                .contains("Read ALL evidence blocks carefully")
                .contains("label | value")
                .contains("even if the wording of the question differs")
                .contains("I could not find this information in the available documents.")
                .contains("Library opening hours | 9:00 AM to 6:00 PM");
        assertThat(prompt.indexOf("Students must maintain 80% attendance."))
                .isLessThan(prompt.indexOf("Question:\nWhat attendance is required?"));
    }

    @Test
    void v3GenerationInstructionDoesNotTreatConflictMetadataAsEvidence() {
        List<ChatMessage> sentMessages = new ArrayList<>();
        ChatLanguageModel stubModel = messages -> {
            sentMessages.addAll(messages);
            return Response.from(AiMessage.from("80% attendance is required."));
        };
        AnswerService answerService = new AnswerService(stubModel);
        SearchService.SearchHit hit = new SearchService.SearchHit(
                UUID.randomUUID(), "Students must maintain 80% attendance.", 3,
                UUID.randomUUID(), "Academic Regulations 2026", "All", "Regulations",
                "2026-27", "v3", LocalDate.of(2026, 6, 15), 0.9123);
        ConflictResolutionService.Resolution resolution = new ConflictResolutionService.Resolution(
                List.of(hit), true, "later_date", "v3", "Academic Regulations 2026", "internal");

        assertThat(answerService.answer("minimum attendance", List.of(hit), resolution))
                .isEqualTo("80% attendance is required.");
        String instruction = ((dev.langchain4j.data.message.UserMessage) sentMessages.get(1)).text();
        assertThat(instruction).doesNotContain("later_date", "Academic Regulations 2026", "authoritative");
    }

    @Test
    void genericEvidenceFallbackUsesQuestionTermsOnly() {
        ChatLanguageModel stubModel = messages -> Response.from(AiMessage.from(AnswerService.NOT_FOUND_ANSWER));
        AnswerService answerService = new AnswerService(stubModel);
        SearchService.SearchHit hit = new SearchService.SearchHit(
                UUID.randomUUID(), "The minimum attendance requirement is 80%.", 3,
                UUID.randomUUID(), "Academic Regulations 2026", "All", "Regulations",
                "2026-27", "v3", LocalDate.of(2026, 6, 15), 0.9123);
        ConflictResolutionService.Resolution resolution = new ConflictResolutionService.Resolution(
                List.of(hit), false, "no_conflict", "v3", "Academic Regulations 2026", "internal");

        assertThat(answerService.answer("minimum attendance", List.of(hit), resolution))
                .contains("minimum attendance requirement is 80%");
    }
}
