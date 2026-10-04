package com.edutrust.ingestion;

import com.edutrust.IntegrationTestBase;
import com.edutrust.database.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IngestionServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void storesChunksWithNonNull384DimensionEmbeddings() throws IOException {
        byte[] pdf = Files.readAllBytes(new ClassPathResource(
                "documents/academic-regulations-2026.pdf").getFile().toPath());
        MockMultipartFile file = new MockMultipartFile(
                "file", "academic-regulations-2026.pdf", "application/pdf", pdf);

        IngestionService.IngestionResult result = ingestionService.ingest(file,
                new IngestionService.DocumentMetadata(
                        "Academic Regulations 2026", "All", "Regulations",
                        "2026-27", "v3", LocalDate.of(2026, 6, 15), "Dean Academics"));
        try {
            Integer chunkCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM chunks WHERE document_id = ?", Integer.class, result.id());
            Integer embeddedChunkCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM chunks WHERE document_id = ? AND embedding IS NOT NULL",
                    Integer.class, result.id());
            Integer dimensions = jdbcTemplate.queryForObject(
                    "SELECT vector_dims(embedding) FROM chunks WHERE document_id = ? LIMIT 1",
                    Integer.class, result.id());

            assertThat(result.pages()).isEqualTo(6);
            assertThat(chunkCount).isEqualTo(result.chunks()).isPositive();
            assertThat(embeddedChunkCount).isEqualTo(chunkCount);
            assertThat(dimensions).isEqualTo(384);
        } finally {
            documentRepository.deleteById(result.id());
        }
    }
}
