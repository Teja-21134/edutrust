package com.edutrust.ingestion;

import com.edutrust.database.ChunkRepository;
import com.edutrust.database.Document;
import com.edutrust.database.DocumentFamily;
import com.edutrust.database.DocumentFamilyRepository;
import com.edutrust.database.DocumentRepository;
import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Runs extraction through vector persistence as one atomic ingestion operation. */
@Service
public class IngestionService {

    private final DocumentRepository documentRepository;
    private final DocumentFamilyRepository documentFamilyRepository;
    private final ChunkRepository chunkRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PdfTextExtractor extractor;
    private final TextCleaner cleaner;
    private final Chunker chunker;
    private final EmbeddingService embeddingService;

    public IngestionService(
            DocumentRepository documentRepository,
            DocumentFamilyRepository documentFamilyRepository,
            ChunkRepository chunkRepository,
            JdbcTemplate jdbcTemplate,
            PdfTextExtractor extractor,
            TextCleaner cleaner,
            Chunker chunker,
            EmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.documentFamilyRepository = documentFamilyRepository;
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
        String documentHash = sha256(file.getBytes());
        documentRepository.findByDocumentHash(documentHash)
                .ifPresent(existing -> {
                    throw new DuplicateDocumentException(existing.getId());
                });

        Document document = new Document(UUID.randomUUID(), metadata.title());
        document.setDepartment(metadata.department());
        document.setDocType(metadata.docType());
        document.setAcademicYear(metadata.academicYear());
        document.setVersion(metadata.version());
        document.setDocDate(metadata.docDate());
        document.setEffectiveDate(metadata.effectiveDate());
        document.setAuthority(metadata.authority());
        document.setFileName(file.getOriginalFilename());
        document.setDocumentHash(documentHash);
        document.setFamily(resolveFamily(metadata));
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

    private DocumentFamily resolveFamily(DocumentMetadata metadata) {
        if (isBlank(metadata.institutionKey()) && isBlank(metadata.familyKey())
                && isBlank(metadata.familyDisplayName())) {
            return null;
        }
        if (isBlank(metadata.institutionKey()) || isBlank(metadata.familyKey())) {
            throw new InvalidFamilyMetadataException();
        }
        String displayName = isBlank(metadata.familyDisplayName())
                ? metadata.familyKey()
                : metadata.familyDisplayName().trim();
        return documentFamilyRepository.findByInstitutionKeyAndFamilyKey(
                        metadata.institutionKey().trim(), metadata.familyKey().trim())
                .orElseGet(() -> documentFamilyRepository.save(new DocumentFamily(
                        UUID.randomUUID(), metadata.institutionKey().trim(), metadata.familyKey().trim(), displayName)));
    }

    private String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record DocumentMetadata(
            String title,
            String department,
            String docType,
            String academicYear,
            String version,
            LocalDate docDate,
            String authority,
            String institutionKey,
            String familyKey,
            String familyDisplayName,
            LocalDate effectiveDate) {

        public DocumentMetadata(
                String title,
                String department,
                String docType,
                String academicYear,
                String version,
                LocalDate docDate,
                String authority) {
            this(title, department, docType, academicYear, version, docDate, authority,
                    null, null, null, null);
        }
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

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class DuplicateDocumentException extends RuntimeException {
        public DuplicateDocumentException(UUID documentId) {
            super("The document already exists: " + documentId);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidFamilyMetadataException extends RuntimeException {
        public InvalidFamilyMetadataException() {
            super("institutionKey and familyKey are both required when family metadata is supplied");
        }
    }
}
