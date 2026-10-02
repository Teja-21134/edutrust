package com.edutrust.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DocumentChunkRepositoryIntegrationTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ChunkRepository chunkRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesReadsAndStoresA384DimensionEmbedding() {
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        Document document = new Document(documentId, "Integration test document");
        Chunk chunk = new Chunk(chunkId, document, 1, 0, "Integration test chunk");

        try {
            documentRepository.saveAndFlush(document);
            chunkRepository.saveAndFlush(chunk);

            String embedding = IntStream.range(0, 384)
                    .mapToObj(index -> "0.0")
                    .collect(Collectors.joining(",", "[", "]"));
            jdbcTemplate.update("UPDATE chunks SET embedding = CAST(? AS vector) WHERE id = ?", embedding, chunkId);

            Document savedDocument = documentRepository.findById(documentId).orElseThrow();
            Chunk savedChunk = chunkRepository.findById(chunkId).orElseThrow();
            Integer embeddingDimensions = jdbcTemplate.queryForObject(
                    "SELECT vector_dims(embedding) FROM chunks WHERE id = ?",
                    Integer.class,
                    chunkId);
            String savedEmbedding = jdbcTemplate.queryForObject(
                    "SELECT embedding::text FROM chunks WHERE id = ?",
                    String.class,
                    chunkId);

            assertThat(savedDocument.getTitle()).isEqualTo("Integration test document");
            assertThat(savedChunk.getText()).isEqualTo("Integration test chunk");
            assertThat(savedEmbedding).isNotNull();
            assertThat(embeddingDimensions).isEqualTo(384);
        } finally {
            documentRepository.deleteById(documentId);
        }
    }
}
