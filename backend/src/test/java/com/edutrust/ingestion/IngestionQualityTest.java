package com.edutrust.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IngestionQualityTest {

    private static final List<String> SAMPLE_PDFS = List.of(
            "academic-regulations-2023.pdf",
            "academic-regulations-2025.pdf",
            "academic-regulations-2026.pdf",
            "academic-calendar-2025-26.pdf",
            "fee-structure-2025-26.pdf",
            "cse-department-circular-2026.pdf");
    private static final List<String> GLUED_TEXT = List.of(
            "isof", "Astudent", "bythe", "Thedates", "March2026", "persemester", "Rs. 500 persemester");

    private PdfTextExtractor extractor;
    private TextCleaner cleaner;
    private Chunker chunker;

    @BeforeEach
    void setUp() {
        extractor = new PdfTextExtractor();
        cleaner = new TextCleaner(0.6);
        chunker = new Chunker(800, 100, 300, 500);
    }

    @Test
    void removesHeadersFootersAndGluedWordsFromAllSampleDocuments() throws IOException {
        for (String name : SAMPLE_PDFS) {
            List<PdfPage> extracted = extractor.extract(resource(name), name);
            List<PdfPage> cleaned = cleaner.clean(extracted);
            List<Chunk> chunks = chunker.chunk(cleaned, name);

            assertThat(extracted).allSatisfy(page -> assertThat(page.text())
                    .doesNotContain("XYZ INSTITUTE OF TECHNOLOGY")
                    .doesNotContain("XIT/")
                    .doesNotContain("Page 3 of"));
            assertThat(chunks).allSatisfy(chunk -> {
                assertThat(chunk.text()).doesNotContain("XYZ INSTITUTE OF TECHNOLOGY");
                assertThat(chunk.text()).doesNotContain("XIT/");
                assertThat(chunk.text()).doesNotContain("Page 3 of");
                GLUED_TEXT.forEach(glued -> assertThat(chunk.text()).doesNotContain(glued));
            });
        }
    }

    @Test
    void keepsInstructionalDaysClauseSeparateFromCreditTable() throws IOException {
        List<Chunk> chunks = chunksFor("academic-regulations-2026.pdf");

        Chunk instructionalDays = chunks.stream()
                .filter(chunk -> chunk.text().contains("90 instructional days"))
                .findFirst()
                .orElseThrow();
        assertThat(instructionalDays.text()).hasSizeLessThan(600);
        assertThat(instructionalDays.text()).startsWith("3. Programme structure");
        assertThat(instructionalDays.text()).doesNotContain("First year | I and II");
    }

    @Test
    void preservesCalendarNotesAsASeparateClause() throws IOException {
        String pageThree = cleaner.clean(extractor.extract(resource("academic-calendar-2025-26.pdf"),
                "academic-calendar-2025-26.pdf")).get(2).text();

        assertThat(pageThree).contains(
                "4.1 The dates in this calendar are subject to change by the Principal. "
                        + "Changes shall be notified through circulars.");
    }

    @Test
    void everyChunkStartsWithAHeadingAndEndsAtASafeBoundary() throws IOException {
        for (String name : SAMPLE_PDFS) {
            for (Chunk chunk : chunksFor(name)) {
                String[] lines = chunk.text().split("\\R");
                String firstLine = lines[0];
                String lastLine = lines[lines.length - 1].trim();
                assertThat(firstLine).matches("(?:\\d+\\.\\s+.*|.*\\|.*)");
                assertThat(lastLine).matches(".*(?:[.!?:;]|\\|.*)$");
            }
        }
    }

    private List<Chunk> chunksFor(String name) throws IOException {
        return chunker.chunk(cleaner.clean(extractor.extract(resource(name), name)), name);
    }

    private InputStream resource(String name) {
        InputStream stream = getClass().getResourceAsStream("/documents/" + name);
        assertThat(stream).as("sample PDF resource " + name).isNotNull();
        return stream;
    }
}
