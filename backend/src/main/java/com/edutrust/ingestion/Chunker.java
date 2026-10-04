package com.edutrust.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Builds page-safe, heading-aware chunks from cleaned PDF pages. */
@Service
public class Chunker {

    private static final Logger log = LoggerFactory.getLogger(Chunker.class);
    private static final Pattern SECTION_HEADING = Pattern.compile("^\\d+\\.\\s+.*");
    private static final Pattern CLAUSE_START = Pattern.compile(
            "^(?:\\d+(?:\\.\\d+)+[.)]?|\\([a-z]\\))\\s+[A-Z].*");
    private static final Pattern TABLE_ROW = Pattern.compile("^.*\\|.*$");

    private final int maxCharacters;
    private final int overlapCharacters;
    private final int targetMinCharacters;
    private final int targetMaxCharacters;

    @Autowired
    public Chunker(
            @Value("${edutrust.chunking.max-chars}") int maxCharacters,
            @Value("${edutrust.chunking.overlap-chars}") int overlapCharacters,
            @Value("${edutrust.chunking.target-min-chars}") int targetMinCharacters,
            @Value("${edutrust.chunking.target-max-chars}") int targetMaxCharacters) {
        if (maxCharacters <= 0 || overlapCharacters < 0 || overlapCharacters >= maxCharacters
                || targetMinCharacters < 0 || targetMaxCharacters <= 0
                || targetMaxCharacters > maxCharacters || targetMinCharacters > targetMaxCharacters) {
            throw new IllegalArgumentException("Invalid chunk size, overlap, or target range");
        }
        this.maxCharacters = maxCharacters;
        this.overlapCharacters = overlapCharacters;
        this.targetMinCharacters = targetMinCharacters;
        this.targetMaxCharacters = targetMaxCharacters;
    }

    public Chunker(int maxCharacters, int overlapCharacters) {
        this(maxCharacters, overlapCharacters, 0, maxCharacters);
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
        String heading = "";
        List<String> clauses = new ArrayList<>();
        List<String> lines = page.text().lines().map(String::trim).filter(line -> !line.isBlank()).toList();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (isSectionHeading(line)) {
                if (!heading.isBlank()) {
                    chunks.addAll(splitClauses(page.pageNumber(), heading, clauses));
                    clauses.clear();
                }
                heading = line;
                continue;
            }
            if (isTableStart(lines, index) && (!heading.isBlank() || clauses.isEmpty())) {
                if (heading.isBlank() && !clauses.isEmpty()) {
                    heading = clauses.removeFirst();
                }
                chunks.addAll(splitClauses(page.pageNumber(), heading, clauses));
                clauses.clear();
                List<String> tableLines = new ArrayList<>();
                tableLines.add(line);
                index++;
                while (index < lines.size() && isTableRow(lines.get(index))) {
                    tableLines.add(lines.get(index));
                    index++;
                }
                index--;
                chunks.addAll(splitTable(page.pageNumber(), heading, tableLines));
                continue;
            }
            if (startsClause(line)) {
                clauses.add(line);
            } else if (!clauses.isEmpty()) {
                int last = clauses.size() - 1;
                clauses.set(last, clauses.get(last) + " " + line);
            } else if (heading.isBlank()) {
                clauses.add(line);
            }
        }
        if (heading.isBlank() && !clauses.isEmpty()) {
            heading = clauses.removeFirst();
        }
        chunks.addAll(splitClauses(page.pageNumber(), heading, clauses));
    }

    private List<Chunk> splitClauses(int pageNumber, String heading, List<String> clauses) {
        if (clauses.isEmpty()) {
            return List.of();
        }
        List<Chunk> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String clause : clauses) {
            if (clause.length() + heading.length() + 1 > maxCharacters) {
                flushClauseGroup(pageNumber, heading, current, result);
                current.setLength(0);
                result.addAll(splitLongClause(pageNumber, heading, clause));
                continue;
            }
            int candidateLength = heading.length() + 1 + current.length()
                    + (current.isEmpty() ? 0 : 1) + clause.length();
            if (!current.isEmpty() && candidateLength > targetMaxCharacters) {
                flushClauseGroup(pageNumber, heading, current, result);
                current.setLength(0);
            }
            if (!current.isEmpty()) {
                current.append('\n');
            }
            current.append(clause);
        }
        flushClauseGroup(pageNumber, heading, current, result);
        return result;
    }

    private void flushClauseGroup(int pageNumber, String heading, StringBuilder current, List<Chunk> result) {
        if (!current.isEmpty()) {
            result.add(new Chunk(-1, pageNumber, withHeading(heading, current.toString())));
        }
    }

    private List<Chunk> splitLongClause(int pageNumber, String heading, String clause) {
        List<Chunk> result = new ArrayList<>();
        int start = 0;
        while (start < clause.length()) {
            int capacity = maxCharacters - heading.length() - 1;
            int end = Math.min(clause.length(), start + capacity);
            if (end < clause.length()) {
                int sentenceEnd = clause.lastIndexOf(". ", end);
                if (sentenceEnd < start) {
                    throw new IllegalArgumentException("A clause has no sentence boundary within the chunk limit");
                }
                end = sentenceEnd + 1;
            }
            String piece = clause.substring(start, end).trim();
            result.add(new Chunk(-1, pageNumber, withHeading(heading, piece)));
            if (end == clause.length()) {
                break;
            }
            int nextStart = end;
            if (overlapCharacters > 0) {
                nextStart = Math.max(start + 1, end - overlapCharacters);
                int sentenceStart = clause.lastIndexOf(". ", nextStart);
                if (sentenceStart >= start) {
                    nextStart = sentenceStart + 2;
                }
            }
            start = nextStart;
        }
        return result;
    }

    private List<Chunk> splitTable(int pageNumber, String heading, List<String> lines) {
        if (lines.size() < 2) {
            return List.of(new Chunk(-1, pageNumber, withHeading(heading, lines.getFirst())));
        }
        String title = lines.getFirst();
        String header = lines.get(1);
        String tablePrefix = title + "\n" + header;
        int capacity = maxCharacters - heading.length() - 1;
        if (tablePrefix.length() > capacity) {
            throw new IllegalArgumentException("Table title and header exceed the configured chunk size");
        }
        List<Chunk> result = new ArrayList<>();
        StringBuilder current = new StringBuilder(tablePrefix);
        for (int index = 2; index < lines.size(); index++) {
            String candidate = current + "\n" + lines.get(index);
            if (candidate.length() > capacity && current.length() > tablePrefix.length()) {
                result.add(new Chunk(-1, pageNumber, withHeading(heading, current.toString())));
                current = new StringBuilder(tablePrefix).append('\n').append(lines.get(index));
            } else {
                current = new StringBuilder(candidate);
            }
        }
        result.add(new Chunk(-1, pageNumber, withHeading(heading, current.toString())));
        return result;
    }

    private boolean isTableStart(List<String> lines, int index) {
        return index + 1 < lines.size() && isTableRow(lines.get(index + 1));
    }

    private boolean isTableRow(String line) {
        return TABLE_ROW.matcher(line).matches();
    }

    private boolean isSectionHeading(String line) {
        return SECTION_HEADING.matcher(line).matches() && !startsClause(line);
    }

    private boolean startsClause(String line) {
        return CLAUSE_START.matcher(line).matches();
    }

    private String withHeading(String heading, String text) {
        return heading.isBlank() ? text : heading + "\n" + text;
    }
}
