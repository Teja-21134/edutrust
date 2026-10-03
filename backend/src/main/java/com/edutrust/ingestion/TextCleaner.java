package com.edutrust.ingestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Removes document boilerplate and makes extracted lines easier to search. */
@Service
public class TextCleaner {

    private static final Pattern PAGE_NUMBER_LINE = Pattern.compile(
            "(?i)^page\\s+\\d+\\s+(?:of|/)\\s+\\d+$");
    private static final Pattern PAGE_NUMBER_FRAGMENT = Pattern.compile(
            "(?i)\\bpage\\s+\\d+\\s+(?:of|/)\\s+\\d+\\b");
    private static final Pattern HEADING = Pattern.compile(
            "^(?:\\d+(?:\\.\\d+)*[.)]?\\s+|[A-Z][A-Z0-9 &:/()'\\-]{4,}$)");
    private static final Pattern TABLE_ROW = Pattern.compile(
            "^(?:[|]|.*\\s{2,}.*|.*\\t.*)$");
    private static final Pattern LIST_OR_NUMBERED_LINE = Pattern.compile(
            "^(?:[-*•]\\s+|\\d+[.)]\\s+).*");
    private static final Pattern TERMINAL_PUNCTUATION = Pattern.compile(".*[.!?:;]$");

    private final double repeatedLinePageRatio;

    public TextCleaner(
            @Value("${app.ingestion.repeated-line-page-ratio}") double repeatedLinePageRatio) {
        if (repeatedLinePageRatio <= 0 || repeatedLinePageRatio > 1) {
            throw new IllegalArgumentException("Repeated-line page ratio must be between 0 and 1");
        }
        this.repeatedLinePageRatio = repeatedLinePageRatio;
    }

    public List<PdfPage> clean(List<PdfPage> pages) {
        if (pages.isEmpty()) {
            return List.of();
        }

        Set<String> repeatedLines = findRepeatedLines(pages);
        return pages.stream()
                .map(page -> new PdfPage(page.pageNumber(), cleanPage(page.text(), repeatedLines)))
                .toList();
    }

    private Set<String> findRepeatedLines(List<PdfPage> pages) {
        Map<String, Integer> pageOccurrences = new HashMap<>();
        for (PdfPage page : pages) {
            Set<String> linesOnPage = page.text().lines()
                    .map(this::lineFingerprint)
                    .filter(line -> !line.isBlank())
                    .collect(Collectors.toSet());
            linesOnPage.forEach(line -> pageOccurrences.merge(line, 1, Integer::sum));
        }

        int minimumPageOccurrences = (int) Math.ceil(pages.size() * repeatedLinePageRatio);
        return pageOccurrences.entrySet().stream()
                .filter(entry -> entry.getValue() >= minimumPageOccurrences)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private String cleanPage(String text, Set<String> repeatedLines) {
        List<String> sourceLines = text.lines().toList();
        List<String> cleanedLines = new ArrayList<>();
        for (String sourceLine : sourceLines) {
            String normalizedLine = normalizeLine(sourceLine);
            String fingerprint = lineFingerprint(sourceLine);
            if (normalizedLine.isBlank() || repeatedLines.contains(fingerprint)
                    || PAGE_NUMBER_LINE.matcher(normalizedLine).matches()) {
                continue;
            }

            normalizedLine = PAGE_NUMBER_FRAGMENT.matcher(normalizedLine).replaceAll("").trim();
            if (normalizedLine.isBlank()) {
                continue;
            }

            if (canJoinToPrevious(cleanedLines, sourceLine, normalizedLine)) {
                int lastIndex = cleanedLines.size() - 1;
                cleanedLines.set(lastIndex, cleanedLines.get(lastIndex) + " " + normalizedLine);
            } else {
                cleanedLines.add(normalizedLine);
            }
        }
        return String.join("\n", cleanedLines);
    }

    private boolean canJoinToPrevious(List<String> cleanedLines, String sourceLine, String normalizedLine) {
        if (cleanedLines.isEmpty()
                || isHeading(cleanedLines.get(cleanedLines.size() - 1))
                || isHeading(normalizedLine)
                || isTableRow(sourceLine)
                || LIST_OR_NUMBERED_LINE.matcher(normalizedLine).matches()) {
            return false;
        }

        String previous = cleanedLines.get(cleanedLines.size() - 1);
        return !TERMINAL_PUNCTUATION.matcher(previous).matches()
                || Character.isLowerCase(normalizedLine.charAt(0));
    }

    private boolean isHeading(String line) {
        return HEADING.matcher(line).find();
    }

    private boolean isTableRow(String line) {
        return TABLE_ROW.matcher(line).matches();
    }

    private String normalizeLine(String line) {
        return line.replaceAll("\\s+", " ").trim();
    }

    private String lineFingerprint(String line) {
        return PAGE_NUMBER_FRAGMENT.matcher(normalizeLine(line)).replaceAll("page # of #");
    }
}
