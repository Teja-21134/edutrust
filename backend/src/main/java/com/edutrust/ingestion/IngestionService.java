package com.edutrust.ingestion;

import com.edutrust.database.ChunkRepository;
import com.edutrust.database.Document;
import com.edutrust.database.DocumentRepository;
import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Runs extraction through vector persistence as one atomic ingestion operation. */
@Service
public class IngestionService {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PdfTextExtractor extractor;
    private final TextCleaner cleaner;
    private final Chunker chunker;
    private final EmbeddingService embeddingService;

    public IngestionService(
            DocumentRepository documentRepository,
            ChunkRepository chunkRepository,
            JdbcTemplate jdbcTemplate,
            PdfTextExtractor extractor,
            TextCleaner cleaner,
            Chunker chunker,
            EmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.extractor = extractor;
        this.cleaner = cleaner;
        this.chunker = chunker;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public IngestionResult ingest(MultipartFile file, DocumentMetadata metadata) throws IOException {
        Instant started = Instant.now();
        Document document = new Document(UUID.randomUUID(), metadata.title());
        document.setDepartment(metadata.department());
        document.setDocType(metadata.docType());
        document.setAcademicYear(metadata.academicYear());
        document.setVersion(metadata.version());
        document.setDocDate(metadata.docDate());
        document.setAuthority(metadata.authority());
        document.setFileName(file.getOriginalFilename());
        documentRepository.saveAndFlush(document);

        List<PdfPage> pages;
        try (var inputStream = file.getInputStream()) {
            pages = extractor.extract(inputStream, file.getOriginalFilename());
        }
        List<PdfPage> cleanedPages = cleaner.clean(pages);
        List<Chunk> chunks = chunker.chunk(cleanedPages, file.getOriginalFilename());
        List<float[]> embeddings = embeddingService.embedAll(chunks.stream().map(Chunk::text).toList());
        if (embeddings.size() != chunks.size()) {
            throw new IllegalStateException("Embedding count did not match chunk count");
        }
        saveChunks(document, chunks, embeddings);

        Duration elapsed = Duration.between(started, Instant.now());
        org.slf4j.LoggerFactory.getLogger(IngestionService.class).info(
                "Ingested document={} id={} pages={} chunks={} elapsedMs={}",
                file.getOriginalFilename(), document.getId(), pages.size(), chunks.size(), elapsed.toMillis());
        return new IngestionResult(document.getId(), document.getTitle(), pages.size(), chunks.size());
    }

    public List<DocumentSummary> listDocuments() {
        return jdbcTemplate.query("""
                SELECT d.id, d.title, d.department, d.doc_type, d.academic_year,
                       d.version, d.doc_date, d.authority, d.file_name,
                       COUNT(c.id) AS chunk_count
                FROM documents d
                LEFT JOIN chunks c ON c.document_id = d.id
                GROUP BY d.id
                ORDER BY d.uploaded_at DESC
                """, (rs, rowNum) -> new DocumentSummary(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("department"),
                rs.getString("doc_type"),
                rs.getString("academic_year"),
                rs.getString("version"),
                rs.getObject("doc_date", LocalDate.class),
                rs.getString("authority"),
                rs.getString("file_name"),
                rs.getLong("chunk_count")));
    }

    @Transactional
    public void deleteDocument(UUID documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }
        documentRepository.deleteById(documentId);
    }

    private void saveChunks(Document document, List<Chunk> chunks, List<float[]> embeddings) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO chunks (id, document_id, page_number, chunk_index, text, embedding)
                VALUES (?, ?, ?, ?, ?, CAST(? AS vector))
                """, chunks, chunks.size(), (statement, chunk) -> {
            int index = chunks.indexOf(chunk);
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, document.getId());
            statement.setInt(3, chunk.pageNumber());
            statement.setInt(4, chunk.chunkIndex());
            statement.setString(5, chunk.text());
            statement.setString(6, pgVectorLiteral(embeddings.get(index)));
        });
    }

    private String pgVectorLiteral(float[] vector) {
        StringBuilder literal = new StringBuilder("[");
        for (int index = 0; index < vector.length; index++) {
            if (index > 0) {
                literal.append(',');
            }
            literal.append(Float.toString(vector[index]));
        }
        return literal.append(']').toString();
    }

    public record DocumentMetadata(
            String title,
            String department,
            String docType,
            String academicYear,
            String version,
            LocalDate docDate,
            String authority) {
    }

    public record IngestionResult(UUID id, String title, int pages, int chunks) {
    }

    public record DocumentSummary(
            UUID id,
            String title,
            String department,
            String docType,
            String academicYear,
            String version,
            LocalDate docDate,
            String authority,
            String fileName,
            long chunks) {
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class DocumentNotFoundException extends RuntimeException {
        public DocumentNotFoundException(UUID documentId) {
            super("Document not found: " + documentId);
        }
    }
}
