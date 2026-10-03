package com.edutrust.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PdfIngestionTest {

    private PdfTextExtractor extractor;
    private TextCleaner cleaner;

    @BeforeEach
    void setUp() {
        extractor = new PdfTextExtractor();
        cleaner = new TextCleaner(0.6);
    }

    @Test
    void extractsAcademicRegulationsPageByPage() throws IOException {
        List<PdfPage> pages = extractor.extract(samplePdf(), "academic-regulations-2026.pdf");

        assertThat(pages).hasSize(6);
        assertThat(pages.get(2).text()).contains("80%", "Condonation");
        assertThat(pages).extracting(PdfPage::pageNumber).containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    void removesRepeatedHeadersAndFootersAndKeepsPageText() throws IOException {
        List<PdfPage> cleanedPages = cleaner.clean(extractor.extract(samplePdf(), "academic-regulations-2026.pdf"));

        String cleanedPageThree = cleanedPages.get(2).text();
        assertThat(cleanedPageThree).contains("80%", "Condonation");
        assertThat(cleanedPages).allSatisfy(page -> assertThat(page.text())
                .doesNotContain("Page " + page.pageNumber() + " of 6")
                .doesNotContain("XYZ INSTITUTE OF TECHNOLOGY"));
    }

    private InputStream samplePdf() {
        InputStream stream = getClass().getResourceAsStream("/documents/academic-regulations-2026.pdf");
        assertThat(stream).as("sample PDF resource").isNotNull();
        return stream;
    }
}
