package com.edutrust.retrieval;

import com.edutrust.database.DocumentRepository;
import com.edutrust.ingestion.IngestionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SearchServiceIntegrationTest {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private DocumentRepository documentRepository;

    private final List<UUID> createdDocumentIds = new ArrayList<>();

    @AfterEach
    void removeTestDocuments() {
        createdDocumentIds.forEach(documentRepository::deleteById);
        createdDocumentIds.clear();
    }

    @Test
    void attendanceSearchFindsPageThreeAmongTopThreeHits() throws IOException {
        UUID documentId = ingestSample();

        List<SearchService.SearchHit> hits = searchService.search("minimum attendance required for exams");

        assertThat(hits).hasSize(5);
        assertThat(hits).allSatisfy(hit -> assertThat(hit.score()).isBetween(0.0, 1.0));
        assertThat(hits).hasSizeGreaterThanOrEqualTo(3);
        assertThat(hits.subList(0, 3)).anySatisfy(hit -> {
            assertThat(hit.documentId()).isEqualTo(documentId);
            assertThat(hit.pageNumber()).isEqualTo(3);
        });
    }

    @Test
    void unrelatedSearchReturnsConfiguredNumberOfHits() throws IOException {
        ingestSample();

        List<SearchService.SearchHit> hits = searchService.search("campus parking permit procedure");

        assertThat(hits).hasSize(5);
        assertThat(hits).allSatisfy(hit -> assertThat(hit.score()).isBetween(0.0, 1.0));
    }

    private UUID ingestSample() throws IOException {
        byte[] pdf = Files.readAllBytes(new ClassPathResource(
                "documents/academic-regulations-2026.pdf").getFile().toPath());
        MockMultipartFile file = new MockMultipartFile(
                "file", "academic-regulations-2026.pdf", "application/pdf", pdf);
        IngestionService.IngestionResult result = ingestionService.ingest(file,
                new IngestionService.DocumentMetadata(
                        "Search Test Academic Regulations 2026", "All", "Regulations",
                        "2026-27", "v3", LocalDate.of(2026, 6, 15), "Dean Academics"));
        createdDocumentIds.add(result.id());
        return result.id();
    }
}
