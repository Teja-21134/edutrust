package com.edutrust.retrieval;

import com.edutrust.ingestion.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Performs vector-only retrieval; ranking and answer generation belong to later tasks. */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;
    private final int topK;
    private final int vectorCandidateK;
    private final int ftsCandidateK;
    private final int rrfK;

    @Autowired
    public SearchService(
            JdbcTemplate jdbcTemplate,
            EmbeddingService embeddingService,
            @Value("${edutrust.retrieval.top-k}") int topK,
            @Value("${edutrust.retrieval.vector-candidate-k}") int vectorCandidateK,
            @Value("${edutrust.retrieval.fts-candidate-k}") int ftsCandidateK,
            @Value("${edutrust.retrieval.rrf-k}") int rrfK) {
        if (topK <= 0) {
            throw new IllegalArgumentException("Retrieval top-k must be positive");
        }
        if (vectorCandidateK <= 0 || ftsCandidateK <= 0 || rrfK <= 0) {
            throw new IllegalArgumentException("Retrieval candidate counts and RRF k must be positive");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
        this.topK = topK;
        this.vectorCandidateK = vectorCandidateK;
        this.ftsCandidateK = ftsCandidateK;
        this.rrfK = rrfK;
    }

    public SearchService(JdbcTemplate jdbcTemplate, EmbeddingService embeddingService, int topK) {
        this(jdbcTemplate, embeddingService, topK, 20, 20, 60);
    }

    public List<SearchHit> search(String question) {
        return searchVector(question, topK);
    }

    public List<SearchHit> searchHybrid(String question) {
        String normalizedQuestion = normalizeQuestion(question);
        Instant started = Instant.now();
        List<SearchHit> vectorHits = searchVector(normalizedQuestion, vectorCandidateK);
        List<SearchHit> fullTextHits = searchFullText(normalizedQuestion);
        Map<UUID, FusedHit> fused = new LinkedHashMap<>();
        addRankedResults(fused, vectorHits);
        addRankedResults(fused, fullTextHits);

        List<FusedHit> ranked = fused.values().stream()
                .sorted(Comparator.comparingDouble(FusedHit::fusedScore).reversed()
                        .thenComparing(hit -> hit.hit().chunkId()))
                .toList();
        Integer explicitYear = detectExplicitYear(normalizedQuestion);
        List<FusedHit> filtered = explicitYear == null ? ranked : preferYear(ranked, explicitYear);
        filtered = preferFamily(filtered, normalizedQuestion);
        List<SearchHit> results = filtered.stream()
                .limit(topK)
                .map(item -> item.hit().withFusedScore(item.fusedScore()))
                .toList();
        log.info("Hybrid search question={} vectorCandidates={} ftsCandidates={} explicitYear={} results={} elapsedMs={}",
                normalizedQuestion, vectorHits.size(), fullTextHits.size(), explicitYear, results.size(),
                Duration.between(started, Instant.now()).toMillis());
        return List.copyOf(results);
    }

    private List<SearchHit> searchVector(String question, int candidateLimit) {
        String normalizedQuestion = normalizeQuestion(question);
        Instant started = Instant.now();
        float[] vector = embeddingService.embed(normalizedQuestion);
        String vectorLiteral = pgVectorLiteral(vector);
        List<SearchHit> hits = jdbcTemplate.query("""
                SELECT c.id AS chunk_id, c.text, c.page_number, d.id AS document_id,
                       d.title, d.department, d.doc_type, d.academic_year,
                       d.version, d.doc_date, d.effective_date, d.authority, f.family_key,
                       1 - (c.embedding <=> CAST(? AS vector)) AS score
                FROM chunks c
                JOIN documents d ON d.id = c.document_id
                LEFT JOIN document_families f ON f.id = d.family_id
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
                rs.getObject("effective_date", LocalDate.class),
                rs.getString("authority"),
                rs.getString("family_key"),
                roundScore(rs.getDouble("score")),
                roundScore(rs.getDouble("score"))),
                vectorLiteral, vectorLiteral, candidateLimit);

        String hitSummary = hits.stream()
                .map(hit -> "%s page=%d score=%.4f".formatted(hit.documentTitle(), hit.pageNumber(), hit.score()))
                .collect(Collectors.joining(", "));
        log.info("Vector search question={} hits=[{}] elapsedMs={}",
                normalizedQuestion, hitSummary, Duration.between(started, Instant.now()).toMillis());
        return List.copyOf(hits);
    }

    /** Retrieves keyword-relevant chunks without changing the existing vector-only path. */
    public List<SearchHit> searchFullText(String question) {
        String normalizedQuestion = question == null ? "" : question.trim();
        if (normalizedQuestion.isBlank()) {
            throw new IllegalArgumentException("Question must not be empty");
        }

        Instant started = Instant.now();
        List<SearchHit> hits = jdbcTemplate.query("""
                SELECT c.id AS chunk_id, c.text, c.page_number, d.id AS document_id,
                       d.title, d.department, d.doc_type, d.academic_year,
                       d.version, d.doc_date, d.effective_date, d.authority, f.family_key,
                       ts_rank_cd(c.search_vector,
                           plainto_tsquery('english'::regconfig, ?)) AS score
                FROM chunks c
                JOIN documents d ON d.id = c.document_id
                LEFT JOIN document_families f ON f.id = d.family_id
                WHERE c.search_vector @@ plainto_tsquery('english'::regconfig, ?)
                ORDER BY score DESC, c.id
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
                rs.getObject("effective_date", LocalDate.class),
                rs.getString("authority"),
                rs.getString("family_key"),
                roundScore(rs.getDouble("score")),
                roundScore(rs.getDouble("score"))),
                normalizedQuestion, normalizedQuestion, ftsCandidateK);

        String hitSummary = hits.stream()
                .map(hit -> "%s page=%d score=%.4f".formatted(hit.documentTitle(), hit.pageNumber(), hit.score()))
                .collect(Collectors.joining(", "));
        log.info("Full-text search question={} hits=[{}] elapsedMs={}",
                normalizedQuestion, hitSummary, Duration.between(started, Instant.now()).toMillis());
        return List.copyOf(hits);
    }

    static double rrfContribution(int rank, int rrfK) {
        if (rank <= 0 || rrfK <= 0) {
            throw new IllegalArgumentException("Rank and RRF k must be positive");
        }
        return 1.0 / (rrfK + rank);
    }

    private void addRankedResults(Map<UUID, FusedHit> fused, List<SearchHit> hits) {
        for (int index = 0; index < hits.size(); index++) {
            SearchHit hit = hits.get(index);
            double contribution = rrfContribution(index + 1, rrfK);
            FusedHit current = fused.get(hit.chunkId());
            if (current == null) {
                fused.put(hit.chunkId(), new FusedHit(hit, contribution));
            } else {
                fused.put(hit.chunkId(), new FusedHit(current.hit(), current.fusedScore() + contribution));
            }
        }
    }

    private List<FusedHit> preferYear(List<FusedHit> candidates, int year) {
        List<FusedHit> matching = candidates.stream()
                .filter(candidate -> matchesYear(candidate.hit(), year))
                .toList();
        return matching.isEmpty() ? candidates : matching;
    }

    private List<FusedHit> preferFamily(List<FusedHit> candidates, String question) {
        String normalizedQuestion = question.toLowerCase().replace('-', ' ');
        List<FusedHit> matching = candidates.stream()
                .filter(candidate -> candidate.hit().familyKey() != null
                        && normalizedQuestion.contains(candidate.hit().familyKey().toLowerCase().replace('-', ' ')))
                .toList();
        return matching.isEmpty() ? candidates : matching;
    }

    private boolean matchesYear(SearchHit hit, int year) {
        String yearText = Integer.toString(year);
        return (hit.academicYear() != null && hit.academicYear().contains(yearText))
                || (hit.version() != null && hit.version().contains(yearText))
                || (hit.docDate() != null && hit.docDate().getYear() == year);
    }

    private Integer detectExplicitYear(String question) {
        for (int year = 2023; year <= 2026; year++) {
            if (question.contains(Integer.toString(year))) {
                return year;
            }
        }
        return null;
    }

    private String normalizeQuestion(String question) {
        String normalizedQuestion = question == null ? "" : question.trim();
        if (normalizedQuestion.isBlank()) {
            throw new IllegalArgumentException("Question must not be empty");
        }
        return normalizedQuestion;
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
            LocalDate effectiveDate,
            String authority,
            String familyKey,
            double score,
            double fusedScore) {

        public SearchHit(
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
            this(chunkId, text, pageNumber, documentId, documentTitle, department, docType,
                    academicYear, version, docDate, null, null, null, score, score);
        }

        public SearchHit withFusedScore(double value) {
            return new SearchHit(chunkId, text, pageNumber, documentId, documentTitle, department,
                    docType, academicYear, version, docDate, effectiveDate, authority,
                    familyKey, score, value);
        }
    }

    private record FusedHit(SearchHit hit, double fusedScore) {
    }
}
