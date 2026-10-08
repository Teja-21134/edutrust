package com.edutrust.generation;

import com.edutrust.config.InstitutionProperties;
import com.edutrust.retrieval.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Selects evidence using metadata before the LLM is called. */
@Service
public class ConflictResolutionService {

    private static final Logger log = LoggerFactory.getLogger(ConflictResolutionService.class);
    private static final Set<String> TOPIC_STOP_WORDS = Set.of(
            "what", "when", "where", "which", "who", "how", "much", "many", "the", "are", "is",
            "a", "an", "of", "for", "to", "in", "on", "under", "required", "please", "tell", "me");
    private static final Pattern FACT = Pattern.compile("\\d+(?:\\.\\d+)?\\s*%|(?:rs\\.?|₹)\\s*[\\d,]+|\\d{1,2}:\\d{2}", Pattern.CASE_INSENSITIVE);

    private final InstitutionProperties institutionProperties;

    public ConflictResolutionService(InstitutionProperties institutionProperties) {
        this.institutionProperties = institutionProperties;
    }

    public Resolution resolve(String question, List<SearchService.SearchHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return new Resolution(List.of(), false, "no_conflict", null, null, "No retrieved evidence");
        }
        Integer requestedYear = requestedYear(question);
        List<SearchService.SearchHit> candidates = requestedYear == null ? hits : hits.stream()
                .filter(hit -> matchesYear(hit, requestedYear))
                .toList();
        if (candidates.isEmpty()) {
            candidates = hits;
        }

        List<SearchService.SearchHit> relevantCandidates = topicRelevantCandidates(question, candidates);
        List<SearchService.SearchHit> documents = distinctDocuments(relevantCandidates);
        if (!documents.isEmpty()) {
            candidates = relevantCandidates;
        }
        boolean conflict = hasConflictingFacts(documents);
        if (!conflict) {
            SearchService.SearchHit selected = candidates.getFirst();
            String rule = requestedYear != null && matchesYear(selected, requestedYear) ? "explicit_year" : "no_conflict";
            return new Resolution(List.of(selected), false, rule, selected.version(),
                    selected.documentTitle(), "Evidence agrees or contains one applicable document");
        }

        List<SearchService.SearchHit> winners = selectWinners(question, candidates, requestedYear);
        if (winners.size() != 1) {
            log.info("V3 unresolved conflict question={} documents={}", question,
                    documents.stream().map(SearchService.SearchHit::documentTitle).toList());
            return new Resolution(List.of(), true, "unresolved_conflict", null, null,
                    "Conflicting documents have no deterministic winner");
        }
        SearchService.SearchHit winner = winners.getFirst();
        String rule = requestedYear != null && matchesYear(winner, requestedYear) ? "explicit_year"
                : resolutionRule(candidates, winner);
        log.info("V3 conflict question={} selected={} rule={} candidates={}", question,
                winner.documentTitle(), rule, documents.stream().map(SearchService.SearchHit::documentTitle).toList());
        return new Resolution(candidates.stream().filter(hit -> hit.documentId().equals(winner.documentId())).toList(),
                true, rule, winner.version(), winner.documentTitle(), "Selected by " + rule);
    }

    private List<SearchService.SearchHit> selectWinners(String question, List<SearchService.SearchHit> hits,
                                                         Integer requestedYear) {
        List<SearchService.SearchHit> documents = distinctDocuments(hits);
        if (requestedYear != null) {
            List<SearchService.SearchHit> requested = documents.stream().filter(hit -> matchesYear(hit, requestedYear)).toList();
            if (requested.size() == 1) return requested;
            if (!requested.isEmpty()) documents = requested;
        }
        Comparator<SearchService.SearchHit> comparator = Comparator
                .comparingInt((SearchService.SearchHit hit) -> overridesDepartmentCircular(hit) ? 1 : 0)
                .thenComparing((SearchService.SearchHit hit) -> applicableDate(hit), Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparingInt(this::authorityRank)
                .thenComparingInt(hit -> hasOverrideClause(hit) ? 1 : 0);
        SearchService.SearchHit winner = documents.stream().max(comparator).orElse(null);
        if (winner == null) return List.of();
        List<SearchService.SearchHit> tied = documents.stream().filter(hit -> comparator.compare(hit, winner) == 0).toList();
        return tied.size() == 1 ? List.of(winner) : List.of();
    }

    private String resolutionRule(List<SearchService.SearchHit> hits, SearchService.SearchHit winner) {
        if (overridesDepartmentCircular(winner)) return "override_clause";
        LocalDate winnerDate = applicableDate(winner);
        if (winnerDate != null && hits.stream().anyMatch(hit -> !hit.documentId().equals(winner.documentId())
                && winnerDate.isAfter(applicableDate(hit) == null ? LocalDate.MIN : applicableDate(hit)))) return "later_date";
        if (hits.stream().anyMatch(hit -> authorityRank(winner) > authorityRank(hit))) return "higher_authority";
        if (hasOverrideClause(winner)) return "override_clause";
        return "unresolved_conflict";
    }

    private boolean hasConflictingFacts(List<SearchService.SearchHit> documents) {
        Set<String> facts = new HashSet<>();
        for (SearchService.SearchHit hit : documents) {
            var matcher = FACT.matcher(hit.text());
            while (matcher.find()) facts.add(matcher.group().replaceAll("\\s+", "").toLowerCase(Locale.ROOT));
        }
        return facts.size() > 1;
    }

    private List<SearchService.SearchHit> distinctDocuments(List<SearchService.SearchHit> hits) {
        List<SearchService.SearchHit> documents = new ArrayList<>();
        Set<Object> seen = new HashSet<>();
        for (SearchService.SearchHit hit : hits) if (seen.add(hit.documentId())) documents.add(hit);
        return documents;
    }

    private List<SearchService.SearchHit> topicRelevantCandidates(String question,
                                                                    List<SearchService.SearchHit> hits) {
        Set<String> topicTerms = topicTerms(question);
        if (topicTerms.isEmpty()) return hits;
        List<SearchService.SearchHit> relevant = hits.stream()
                .filter(hit -> {
                    String searchableText = String.join(" ",
                            hit.text() == null ? "" : hit.text(),
                            hit.documentTitle() == null ? "" : hit.documentTitle(),
                            hit.docType() == null ? "" : hit.docType(),
                            hit.department() == null ? "" : hit.department()).toLowerCase(Locale.ROOT);
                    return topicTerms.stream().anyMatch(searchableText::contains);
                })
                .toList();
        return relevant.isEmpty() ? hits : relevant;
    }

    private Set<String> topicTerms(String question) {
        Set<String> terms = new HashSet<>();
        var matcher = Pattern.compile("[a-zA-Z]{3,}").matcher(question == null ? "" : question.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String term = matcher.group();
            if (!TOPIC_STOP_WORDS.contains(term)) {
                terms.add(term.endsWith("ies") ? term.substring(0, term.length() - 3) + "y"
                        : term.endsWith("s") ? term.substring(0, term.length() - 1) : term);
            }
        }
        return terms;
    }

    private boolean matchesYear(SearchService.SearchHit hit, int year) {
        return (hit.academicYear() != null && hit.academicYear().contains(Integer.toString(year)))
                || (hit.version() != null && hit.version().contains(Integer.toString(year)))
                || (hit.docDate() != null && hit.docDate().getYear() == year);
    }

    private Integer requestedYear(String question) {
        var matcher = Pattern.compile("\\b(20\\d{2})\\b").matcher(question == null ? "" : question);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private LocalDate applicableDate(SearchService.SearchHit hit) {
        return hit.effectiveDate() != null ? hit.effectiveDate() : hit.docDate();
    }

    private int authorityRank(SearchService.SearchHit hit) {
        return institutionProperties.getDocumentTypes().stream()
                .filter(type -> type.getName().equalsIgnoreCase(hit.docType() == null ? "" : hit.docType()))
                .mapToInt(InstitutionProperties.DocumentType::getAuthorityRank)
                .findFirst().orElse(institutionProperties.getDefaultAuthorityRank());
    }

    private boolean hasOverrideClause(SearchService.SearchHit hit) {
        String text = hit.text().toLowerCase(Locale.ROOT);
        return institutionProperties.getOverridePhrases().stream().anyMatch(text::contains);
    }

    private boolean overridesDepartmentCircular(SearchService.SearchHit hit) {
        String text = hit.text().toLowerCase(Locale.ROOT);
        return text.contains("department circular") && hasOverrideClause(hit);
    }

    public record Resolution(List<SearchService.SearchHit> selectedEvidence, boolean conflictDetected,
                             String resolution, String selectedVersion, String selectedDocument, String explanation) {
    }
}
