package com.edutrust.generation;

import com.edutrust.retrieval.SearchService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Performs a small, explainable evidence check after answer generation. */
@Service
public class FaithfulnessVerifier {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "in", "is",
            "it", "of", "on", "or", "that", "the", "this", "to", "under", "was", "what",
            "when", "where", "which", "with");

    private final double minimumScore;

    public FaithfulnessVerifier(@Value("${edutrust.verification.minimum-score}") double minimumScore) {
        this.minimumScore = minimumScore;
    }

    public Verification verify(String answer, List<SearchService.SearchHit> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return new Verification("NOT_FOUND", 0.0, "No retrieved evidence");
        }
        if (answer == null || answer.isBlank() || AnswerService.NOT_FOUND_ANSWER.equalsIgnoreCase(answer.trim())) {
            return new Verification("NOT_FOUND", 0.0, "The answer contains no supported finding");
        }

        String answerText = normalize(answer);
        String evidenceText = normalize(evidence.stream().map(SearchService.SearchHit::text)
                .collect(Collectors.joining(" ")));
        List<String> factualTokens = tokens(answerText);
        long supported = factualTokens.stream().filter(evidenceText::contains).count();
        boolean hasNumber = answerText.matches(".*\\d.*");
        List<String> numbers = Arrays.stream(answerText.split("\\D+"))
                .filter(token -> !token.isBlank()).toList();
        boolean numberSupported = !hasNumber || numbers.stream().allMatch(evidenceText::contains);
        double score = factualTokens.isEmpty() ? 0.0 : (double) supported / factualTokens.size();
        if (numberSupported && score >= minimumScore) {
            return new Verification("VERIFIED", Math.min(1.0, score), "Answer facts occur in selected evidence");
        }
        return new Verification("UNVERIFIED", score, "Answer facts are not adequately supported by selected evidence");
    }

    private List<String> tokens(String text) {
        return Arrays.stream(text.split("\\W+"))
                .filter(token -> (token.length() > 2 || token.chars().allMatch(Character::isDigit))
                        && !STOP_WORDS.contains(token))
                .toList();
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    public record Verification(String status, double score, String reason) {
    }
}
