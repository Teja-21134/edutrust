package com.edutrust.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Builds page-safe, heading-aware chunks from cleaned PDF pages. */
@Service
public class Chunker {

    private static final Logger log = LoggerFactory.getLogger(Chunker.class);
    private static final Pattern SECTION_HEADING = Pattern.compile("^\\d+\\.\\s+.*");
    private static final Pattern NUMBERED_CLAUSE = Pattern.compile("^\\d+(?:\\.\\d+)+[.)]?\\s+.*");
    private static final Pattern TABLE_ROW = Pattern.compile(
            "^(?:\\|.*\\||\\d+\\s+to\\s+\\d+\\s+.*|(?:Below|Absent)\\b.*)$", Pattern.CASE_INSENSITIVE);

    private final int maxCharacters;
    private final int overlapCharacters;

    public Chunker(
            @Value("${edutrust.chunking.max-chars}") int maxCharacters,
            @Value("${edutrust.chunking.overlap-chars}") int overlapCharacters) {
        if (maxCharacters <= 0 || overlapCharacters < 0 || overlapCharacters >= maxCharacters) {
            throw new IllegalArgumentException("Chunk size must be positive and overlap must be smaller than chunk size");
        }
        this.maxCharacters = maxCharacters;
        this.overlapCharacters = overlapCharacters;
    }

    public List<Chunk> chunk(List<PdfPage> pages) {
        return chunk(pages, "document");
    }

    public List<Chunk> chunk(List<PdfPage> pages, String documentName) {
        List<Chunk> chunks = new ArrayList<>();
        for (PdfPage page : pages) {
            chunkPage(page, chunks);
        }

        List<Chunk> indexedChunks = new ArrayList<>(chunks.size());
        for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
            Chunk chunk = chunks.get(chunkIndex);
            indexedChunks.add(new Chunk(chunkIndex, chunk.pageNumber(), chunk.text()));
        }

        double averageLength = indexedChunks.stream()
                .mapToInt(chunk -> chunk.text().length())
                .average()
                .orElse(0.0);
        log.info("Chunked document={} chunks={} averageLength={}",
                documentName, indexedChunks.size(), String.format("%.1f", averageLength));
        return List.copyOf(indexedChunks);
    }

    private void chunkPage(PdfPage page, List<Chunk> chunks) {
        String currentHeading = "";
        StringBuilder pendingText = new StringBuilder();
        boolean pendingClause = false;
        List<String> lines = page.text().lines().toList();

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex).trim();
            if (line.isBlank()) {
                continue;
            }

            if (isSectionHeading(line)) {
                chunks.addAll(flush(page.pageNumber(), currentHeading, pendingText, pendingClause));
                pendingText.setLength(0);
                currentHeading = line;
                pendingClause = false;
                continue;
            }

            if (isTableHeading(lines, lineIndex)) {
                chunks.addAll(flush(page.pageNumber(), currentHeading, pendingText, pendingClause));
                pendingText.setLength(0);
                StringBuilder table = new StringBuilder(line);
                lineIndex++;
                while (lineIndex < lines.size() && isTableRow(lines.get(lineIndex).trim())) {
                    table.append('\n').append(lines.get(lineIndex).trim());
                    lineIndex++;
                }
                lineIndex--;
                chunks.addAll(addUnit(page.pageNumber(), currentHeading, table.toString(), false, true));
                continue;
            }

            if (isNumberedClause(line)) {
                if (pendingText.length() > 0 && !fits(currentHeading, pendingText + "\n" + line)) {
                    chunks.addAll(flush(page.pageNumber(), currentHeading, pendingText, pendingClause));
                    pendingClause = false;
                }
                pendingClause = true;
            }

            if (pendingText.length() > 0) {
                pendingText.append('\n');
            }
            pendingText.append(line);
        }
        chunks.addAll(flush(page.pageNumber(), currentHeading, pendingText, pendingClause));
    }

    private List<Chunk> flush(int pageNumber, String heading, StringBuilder text, boolean clause) {
        if (text.isEmpty()) {
            return List.of();
        }
        List<Chunk> result = addUnit(pageNumber, heading, text.toString(), clause, false);
        text.setLength(0);
        return result;
    }

    private List<Chunk> addUnit(int pageNumber, String heading, String text, boolean clause, boolean table) {
        String prefix = heading.isBlank() ? "" : heading + "\n";
        int contentCapacity = maxCharacters - prefix.length();
        if (prefix.length() >= maxCharacters) {
            throw new IllegalArgumentException("Section heading is longer than the configured chunk size");
        }

        if (prefix.length() + text.length() <= maxCharacters) {
            return List.of(new Chunk(-1, pageNumber, prefix + text));
        }

        if (table) {
            return splitTable(pageNumber, heading, text, contentCapacity);
        }
        return splitText(pageNumber, heading, text, contentCapacity, clause);
    }

    private List<Chunk> splitText(int pageNumber, String heading, String text, int contentCapacity, boolean clause) {
        List<Chunk> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + contentCapacity);
            if (end < text.length()) {
                int boundary = text.lastIndexOf(' ', end);
                if (boundary > start) {
                    end = boundary;
                }
            }
            String piece = text.substring(start, end).trim();
            result.add(new Chunk(-1, pageNumber, withHeading(heading, piece)));
            if (end == text.length()) {
                break;
            }
            int nextStart = end;
            if (clause) {
                nextStart = Math.max(start + 1, end - overlapCharacters);
                while (nextStart < text.length() && text.charAt(nextStart) == ' ') {
                    nextStart++;
                }
            }
            start = nextStart;
        }
        return result;
    }

    private List<Chunk> splitTable(int pageNumber, String heading, String text, int contentCapacity) {
        List<Chunk> result = new ArrayList<>();
        String[] rows = text.split("\\R");
        String tableHeading = rows[0];
        StringBuilder current = new StringBuilder(tableHeading);
        for (int rowIndex = 1; rowIndex < rows.length; rowIndex++) {
            String candidate = current + "\n" + rows[rowIndex];
            if (candidate.length() > contentCapacity && current.length() > tableHeading.length()) {
                result.add(new Chunk(-1, pageNumber, withHeading(heading, current.toString())));
                current = new StringBuilder(tableHeading).append('\n').append(rows[rowIndex]);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (current.length() > contentCapacity) {
            throw new IllegalArgumentException("A table row is longer than the configured chunk size");
        }
        result.add(new Chunk(-1, pageNumber, withHeading(heading, current.toString())));
        return result;
    }

    private String withHeading(String heading, String text) {
        return heading.isBlank() ? text : heading + "\n" + text;
    }

    private boolean fits(String heading, String text) {
        return withHeading(heading, text).length() <= maxCharacters;
    }

    private boolean isSectionHeading(String line) {
        return SECTION_HEADING.matcher(line).matches() && !isNumberedClause(line);
    }

    private boolean isNumberedClause(String line) {
        return NUMBERED_CLAUSE.matcher(line).matches();
    }

    private boolean isTableHeading(List<String> lines, int index) {
        if (index + 1 >= lines.size() || isSectionHeading(lines.get(index)) || isNumberedClause(lines.get(index))) {
            return false;
        }
        String line = lines.get(index).trim();
        String nextLine = lines.get(index + 1).trim();
        return (line.contains("|") || line.toLowerCase().contains("grade") || line.toLowerCase().contains("amount"))
                && isTableRow(nextLine);
    }

    private boolean isTableRow(String line) {
        return TABLE_ROW.matcher(line).matches();
    }
}
