package com.edutrust.retrieval;

import com.edutrust.ingestion.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Performs vector-only retrieval; ranking and answer generation belong to later tasks. */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;
    private final int topK;

    public SearchService(
            JdbcTemplate jdbcTemplate,
            EmbeddingService embeddingService,
            @Value("${edutrust.retrieval.top-k}") int topK) {
        if (topK <= 0) {
            throw new IllegalArgumentException("Retrieval top-k must be positive");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
        this.topK = topK;
    }

    public List<SearchHit> search(String question) {
        String normalizedQuestion = question == null ? "" : question.trim();
        if (normalizedQuestion.isBlank()) {
            throw new IllegalArgumentException("Question must not be empty");
        }

        Instant started = Instant.now();
        float[] vector = embeddingService.embed(normalizedQuestion);
        String vectorLiteral = pgVectorLiteral(vector);
        List<SearchHit> hits = jdbcTemplate.query("""
                SELECT c.id AS chunk_id, c.text, c.page_number, d.id AS document_id,
                       d.title, d.department, d.doc_type, d.academic_year,
                       d.version, d.doc_date,
                       1 - (c.embedding <=> CAST(? AS vector)) AS score
                FROM chunks c
                JOIN documents d ON d.id = c.document_id
                WHERE c.embedding IS NOT NULL
                ORDER BY c.embedding <=> CAST(? AS vector)
                LIMIT ?
                """, (rs, rowNum) -> new SearchHit(
                rs.getObject("chunk_id", UUID.class),
                rs.getString("text"),
                rs.getInt("page_number"),
                rs.getObject("document_id", UUID.class),
                rs.getString("title"),
                rs.getString("department"),
                rs.getString("doc_type"),
                rs.getString("academic_year"),
                rs.getString("version"),
                rs.getObject("doc_date", LocalDate.class),
                roundScore(rs.getDouble("score"))),
                vectorLiteral, vectorLiteral, topK);

        String hitSummary = hits.stream()
                .map(hit -> "%s page=%d score=%.4f".formatted(hit.documentTitle(), hit.pageNumber(), hit.score()))
                .collect(Collectors.joining(", "));
        log.info("Vector search question={} hits=[{}] elapsedMs={}",
                normalizedQuestion, hitSummary, Duration.between(started, Instant.now()).toMillis());
        return List.copyOf(hits);
    }

    private double roundScore(double score) {
        double boundedScore = Math.max(0.0, Math.min(1.0, score));
        return BigDecimal.valueOf(boundedScore).setScale(4, RoundingMode.HALF_UP).doubleValue();
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

    public record SearchHit(
            UUID chunkId,
            String text,
            int pageNumber,
            UUID documentId,
            String documentTitle,
            String department,
            String docType,
            String academicYear,
            String version,
            LocalDate docDate,
            double score) {
    }
}
