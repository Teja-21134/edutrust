package com.edutrust.generation;

import com.edutrust.config.InstitutionProperties;
import com.edutrust.retrieval.SearchService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class V3TrustLayerTest {

    @Test
    void explicitYearBeatsNewerConflictingRegulation() {
        ConflictResolutionService service = new ConflictResolutionService(properties());
        var result = service.resolve("minimum attendance under Academic Regulations 2023", List.of(
                hit("2026", "Students must maintain 80% attendance.", LocalDate.of(2026, 6, 15)),
                hit("2023", "Students must maintain 75% attendance.", LocalDate.of(2023, 6, 15))));

        assertThat(result.selectedDocument()).isEqualTo("Regulations 2023");
        assertThat(result.resolution()).isEqualTo("explicit_year");
    }

    @Test
    void laterApplicableDocumentWinsConflict() {
        ConflictResolutionService service = new ConflictResolutionService(properties());
        var result = service.resolve("minimum attendance required", List.of(
                hit("2023", "Students must maintain 75% attendance.", LocalDate.of(2023, 6, 15)),
                hit("2026", "Students must maintain 80% attendance.", LocalDate.of(2026, 6, 15))));

        assertThat(result.selectedDocument()).isEqualTo("Regulations 2026");
        assertThat(result.resolution()).isEqualTo("later_date");
    }

    @Test
    void equalMetadataLeavesConflictUnresolved() {
        ConflictResolutionService service = new ConflictResolutionService(properties());
        var date = LocalDate.of(2026, 6, 15);
        var result = service.resolve("minimum attendance required", List.of(
                hit("A", "Students must maintain 75% attendance.", date),
                hit("B", "Students must maintain 80% attendance.", date)));

        assertThat(result.selectedEvidence()).isEmpty();
        assertThat(result.resolution()).isEqualTo("unresolved_conflict");
    }

    @Test
    void verifierDistinguishesSupportedUnsupportedAndMissingEvidence() {
        FaithfulnessVerifier verifier = new FaithfulnessVerifier(0.55);
        var evidence = List.of(hit("2026", "Students must maintain 80% attendance.", LocalDate.of(2026, 6, 15)));

        assertThat(verifier.verify("80% attendance is required.", evidence).status()).isEqualTo("VERIFIED");
        assertThat(verifier.verify("The fee is Rs. 1000.", evidence).status()).isEqualTo("UNVERIFIED");
        assertThat(verifier.verify("Anything", List.of()).status()).isEqualTo("NOT_FOUND");
    }

    @Test
    void exactCseHackathonConflictUsesOverridingAcademicRegulation() {
        String question = "How much on-duty attendance credit can CSE students get for hackathons?";
        SearchService searchService = new SearchService(null, null, 2) {
            @Override
            public List<SearchHit> searchHybrid(String ignored) {
                return List.of(
                        hit("v3", "Students representing the department in hackathons shall be marked on duty. "
                                + "The on-duty credit for such events shall be up to 10% of the total classes held.",
                                LocalDate.of(2026, 8, 1), "CSE Department Circular 2026", "Circular"),
                        hit("v3", "The on-duty credit shall not exceed 5% of the total classes held in the semester. "
                                + "Any department circular that conflicts with this clause stands overridden.",
                                LocalDate.of(2026, 6, 1), "Academic Regulations 2026", "Regulations"));
            }
        };
        List<ChatMessage> modelMessages = new ArrayList<>();
        AnswerService answerService = new AnswerService(messages -> {
            modelMessages.addAll(messages);
            return Response.from(AiMessage.from(AnswerService.NOT_FOUND_ANSWER));
        });

        ConflictResolutionService actualResolver = new ConflictResolutionService(propertiesWithCircular());
        ConflictResolutionService resolverWithEmptySelectedEvidence = new ConflictResolutionService(propertiesWithCircular()) {
            @Override
            public Resolution resolve(String ignored, List<SearchService.SearchHit> hits) {
                Resolution selected = actualResolver.resolve(ignored, hits);
                return new Resolution(List.of(), selected.conflictDetected(), selected.resolution(),
                        selected.selectedVersion(), selected.selectedDocument(), selected.explanation());
            }
        };
        AskService.AskResult result = new AskService(searchService, answerService,
                resolverWithEmptySelectedEvidence,
                new FaithfulnessVerifier(0.55)).ask(question, "v3");

        assertThat(result.answer()).contains("5%");
        assertThat(result.answered()).isTrue();
        assertThat(result.faithfulnessStatus()).isEqualTo("VERIFIED");
        assertThat(result.faithfulnessScore()).isGreaterThanOrEqualTo(0.55);
        assertThat(result.conflictDetected()).isTrue();
        assertThat(result.conflictResolution()).isEqualTo("override_clause");
        assertThat(result.selectedDocument()).isEqualTo("Academic Regulations 2026");
        assertThat(result.selectedVersion()).isEqualTo("v3");
        assertThat(result.sources()).containsExactly(new AskService.Source("Academic Regulations 2026", 3, "v3"));
        assertThat(modelMessages).anySatisfy(message -> {
            if (message instanceof UserMessage userMessage) {
                assertThat(userMessage.text()).contains("override_clause", "Academic Regulations 2026", "authoritative");
            }
        });
    }

    private SearchService.SearchHit hit(String version, String text, LocalDate date) {
        return hit(version, text, date, "Regulations " + version, "Regulations");
    }

    private SearchService.SearchHit hit(String version, String text, LocalDate date, String title, String docType) {
        return new SearchService.SearchHit(UUID.randomUUID(), text, 3, UUID.randomUUID(),
                title, "All", docType, version + "-27", version,
                date, date, "Dean Academics", "academic-regulations", 0.9, 0.9);
    }

    private InstitutionProperties properties() {
        InstitutionProperties properties = new InstitutionProperties();
        InstitutionProperties.DocumentType type = new InstitutionProperties.DocumentType();
        type.setName("Regulations");
        type.setAuthorityRank(3);
        InstitutionProperties.DocumentType circular = new InstitutionProperties.DocumentType();
        circular.setName("Circular");
        circular.setAuthorityRank(1);
        properties.setDocumentTypes(List.of(type, circular));
        properties.setDefaultAuthorityRank(1);
        properties.setOverridePhrases(List.of("stands overridden"));
        return properties;
    }

    private InstitutionProperties propertiesWithCircular() {
        return properties();
    }
}
