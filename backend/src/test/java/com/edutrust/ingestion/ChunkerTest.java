package com.edutrust.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

class ChunkerTest {

    private static final int MAX_CHARS = 800;
    private static final int OVERLAP_CHARS = 100;

    @Test
    void chunksCleanedAcademicRegulationsWithoutCrossingPages() throws IOException {
        PdfTextExtractor extractor = new PdfTextExtractor();
        TextCleaner cleaner = new TextCleaner(0.6);
        Chunker chunker = new Chunker(MAX_CHARS, OVERLAP_CHARS);

        List<Chunk> chunks = chunker.chunk(
                cleaner.clean(extractor.extract(samplePdf(), "academic-regulations-2026.pdf")),
                "academic-regulations-2026.pdf");

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.text().length()).isLessThanOrEqualTo(MAX_CHARS));
        assertThat(chunks).extracting(Chunk::chunkIndex)
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, chunks.size()).boxed().toList());
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.pageNumber()).isBetween(1, 6));

        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.text()).doesNotContain("XYZ INSTITUTE OF TECHNOLOGY"));

        Chunk attendanceChunk = chunks.stream()
                .filter(chunk -> chunk.text().contains("80%"))
                .findFirst()
                .orElseThrow();
        assertThat(attendanceChunk.pageNumber()).isEqualTo(3);
        assertThat(attendanceChunk.text()).startsWith("4. Attendance requirements");
    }

    @Test
    void repeatsTableTitleAndHeaderWhenTableSpansChunks() {
        Chunker chunker = new Chunker(45, 10);
        List<Chunk> chunks = chunker.chunk(List.of(new PdfPage(1, "Fees\nItem | Amount\nTuition | Rs. 100\nHostel | Rs. 200\nLibrary | Rs. 300")), "fees.pdf");

        assertThat(chunks).hasSize(3);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.text()).contains("Fees\nItem | Amount");
            assertThat(chunk.text()).doesNotContain("Tuition | Rs. 100\nHostel | Rs. 200");
        });
    }

    private InputStream samplePdf() {
        InputStream stream = getClass().getResourceAsStream("/documents/academic-regulations-2026.pdf");
        assertThat(stream).as("sample PDF resource").isNotNull();
        return stream;
    }
}
