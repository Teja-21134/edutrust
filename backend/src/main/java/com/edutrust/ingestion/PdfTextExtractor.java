package com.edutrust.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Extracts text one page at a time so page citations remain available. */
@Service
public class PdfTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractor.class);

    public List<PdfPage> extract(Path pdfFile) throws IOException {
        try (InputStream inputStream = Files.newInputStream(pdfFile)) {
            return extract(inputStream, pdfFile.getFileName().toString());
        }
    }

    public List<PdfPage> extract(InputStream inputStream, String documentName) throws IOException {
        byte[] pdfBytes = inputStream.readAllBytes();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<PdfPage> pages = new ArrayList<>(document.getNumberOfPages());
            int characterCount = 0;

            for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                String text = stripper.getText(document).strip();
                pages.add(new PdfPage(pageNumber, text));
                characterCount += text.length();
            }

            log.info("Extracted document={} pages={} characters={}", documentName, pages.size(), characterCount);
            return List.copyOf(pages);
        }
    }
}
