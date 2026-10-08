package com.edutrust.retrieval;

import com.edutrust.IntegrationTestBase;
import com.edutrust.ingestion.IngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SearchServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private SearchService searchService;

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

    @Test
    void fullTextSearchFindsKeywordRelevantChunks() throws IOException {
        ingestSample();

        List<SearchService.SearchHit> hits = searchService.searchFullText(
                "minimum attendance required for semester-end examinations");

        assertThat(hits).isNotEmpty();
        assertThat(hits).allSatisfy(hit -> assertThat(hit.text()).containsIgnoringCase("attendance"));
        assertThat(hits).anySatisfy(hit -> assertThat(hit.pageNumber()).isEqualTo(3));
    }

    @Test
    void hybridSearchReturnsAtMostFiveResultsAndKeepsUnversionedDocumentsSearchable() throws IOException {
        ingestSample("academic-regulations-2026.pdf", "Search Test Academic Regulations 2026", "2026-27", "v3");

        List<SearchService.SearchHit> hits = searchService.searchHybrid("minimum attendance required");

        assertThat(hits).isNotEmpty().hasSizeLessThanOrEqualTo(5);
        assertThat(hits).allSatisfy(hit -> assertThat(hit.fusedScore()).isPositive());
    }

    @Test
    void hybridSearchPrefersExplicit2023Metadata() throws IOException {
        ingestSample("academic-regulations-2023.pdf", "Search Test Academic Regulations 2023", "2023-24", "v1");
        ingestSample("academic-regulations-2026.pdf", "Search Test Academic Regulations 2026", "2026-27", "v3");

        List<SearchService.SearchHit> hits = searchService.searchHybrid(
                "minimum attendance under Academic Regulations 2023");

        assertThat(hits).isNotEmpty();
        assertThat(hits).allSatisfy(hit -> assertThat(hit.academicYear()).contains("2023"));
    }

    private UUID ingestSample() throws IOException {
        return ingestSample("academic-regulations-2026.pdf", "Search Test Academic Regulations 2026", "2026-27", "v3");
    }

    private UUID ingestSample(String fileName, String title, String academicYear, String version) throws IOException {
        byte[] pdf = Files.readAllBytes(new ClassPathResource("documents/" + fileName).getFile().toPath());
        MockMultipartFile file = new MockMultipartFile(
                "file", fileName, "application/pdf", pdf);
        IngestionService.IngestionResult result = ingestionService.ingest(file,
                new IngestionService.DocumentMetadata(
                        title, "All", "Regulations", academicYear, version,
                        LocalDate.of(Integer.parseInt(academicYear.substring(0, 4)), 6, 15), "Dean Academics"));
        return result.id();
    }
}
