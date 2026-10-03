package com.edutrust.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        Map<Integer, String> cleanedTextByPage = cleaner.clean(
                        extractor.extract(samplePdf(), "academic-regulations-2026.pdf"))
                .stream()
                .collect(Collectors.toMap(PdfPage::pageNumber, PdfPage::text));
        chunks.forEach(chunk -> chunk.text().lines()
                .skip(1)
                .forEach(line -> assertThat(cleanedTextByPage.get(chunk.pageNumber())).contains(line)));

        Chunk attendanceChunk = chunks.stream()
                .filter(chunk -> chunk.text().contains("80%"))
                .findFirst()
                .orElseThrow();
        assertThat(attendanceChunk.pageNumber()).isEqualTo(3);
        assertThat(attendanceChunk.text()).startsWith("4. Attendance requirements");
    }

    private InputStream samplePdf() {
        InputStream stream = getClass().getResourceAsStream("/documents/academic-regulations-2026.pdf");
        assertThat(stream).as("sample PDF resource").isNotNull();
        return stream;
    }
}
