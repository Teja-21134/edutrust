package com.edutrust.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
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

    @org.springframework.beans.factory.annotation.Value("${app.ingestion.pdf.spacing-tolerance:0.25}")
    private float spacingTolerance;

    @org.springframework.beans.factory.annotation.Value("${app.ingestion.pdf.average-char-tolerance:0.25}")
    private float averageCharTolerance;

    @org.springframework.beans.factory.annotation.Value("${app.ingestion.pdf.column-gap-points:8}")
    private float columnGapPoints;

    @org.springframework.beans.factory.annotation.Value("${app.ingestion.pdf.line-y-tolerance-points:2.5}")
    private float lineYTolerancePoints;

    public List<PdfPage> extract(Path pdfFile) throws IOException {
        try (InputStream inputStream = Files.newInputStream(pdfFile)) {
            return extract(inputStream, pdfFile.getFileName().toString());
        }
    }

    public List<PdfPage> extract(InputStream inputStream, String documentName) throws IOException {
        byte[] pdfBytes = inputStream.readAllBytes();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            List<PdfPage> pages = new ArrayList<>(document.getNumberOfPages());
            int characterCount = 0;

            for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
                PositionAwareStripper stripper = new PositionAwareStripper();
                stripper.setSortByPosition(true);
                stripper.setSpacingTolerance(spacingTolerance);
                stripper.setAverageCharTolerance(averageCharTolerance);
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                stripper.getText(document);
                String text = rebuildLines(stripper.runs()).strip();
                pages.add(new PdfPage(pageNumber, text));
                characterCount += text.length();
            }

            log.info("Extracted document={} pages={} characters={}", documentName, pages.size(), characterCount);
            return List.copyOf(pages);
        }
    }

    private String rebuildLines(List<TextRun> runs) {
        List<List<TextRun>> lines = new ArrayList<>();
        for (TextRun run : runs) {
            List<TextRun> line = lines.stream()
                    .filter(candidate -> Math.abs(candidate.get(0).y() - run.y()) <= lineYTolerancePoints)
                    .findFirst()
                    .orElseGet(() -> {
                        List<TextRun> created = new ArrayList<>();
                        lines.add(created);
                        return created;
                    });
            line.add(run);
        }

        lines.sort(Comparator.comparingDouble(line -> line.get(0).y()));
        List<RenderedLine> rebuilt = new ArrayList<>();
        for (List<TextRun> line : lines) {
            line.sort(Comparator.comparingDouble(TextRun::x));
            StringBuilder text = new StringBuilder();
            TextRun prior = null;
            for (TextRun run : line) {
                if (prior != null) {
                    float gap = run.x() - prior.endX();
                    if (gap > columnGapPoints) {
                        text.append(" | ");
                    } else if (!endsWithWhitespace(text) && gap > 0) {
                        text.append(' ');
                    }
                }
                text.append(run.text());
                prior = run;
            }
            String value = text.toString().replaceAll("\\s+", " ").trim();
            if (!value.isBlank()) {
                float minimumX = (float) line.stream().mapToDouble(TextRun::x).min().orElse(0);
                if (!rebuilt.isEmpty() && rebuilt.get(rebuilt.size() - 1).text().contains(" | ")
                        && !value.contains(" | ")
                        && Math.abs(rebuilt.get(rebuilt.size() - 1).minimumX() - minimumX) <= columnGapPoints) {
                    RenderedLine previous = rebuilt.remove(rebuilt.size() - 1);
                    rebuilt.add(new RenderedLine(previous.text() + " " + value, previous.minimumX()));
                } else {
                    rebuilt.add(new RenderedLine(value, minimumX));
                }
            }
        }
        return rebuilt.stream().map(RenderedLine::text).collect(java.util.stream.Collectors.joining("\n"));
    }

    private boolean endsWithWhitespace(StringBuilder text) {
        return text.length() > 0 && Character.isWhitespace(text.charAt(text.length() - 1));
    }

    private static final class PositionAwareStripper extends PDFTextStripper {
        private final List<TextRun> runs = new ArrayList<>();

        private PositionAwareStripper() throws IOException {
            super();
        }

        @Override
        protected void writeString(String text, List<org.apache.pdfbox.text.TextPosition> textPositions)
                throws IOException {
            if (text == null || text.isBlank() || textPositions == null || textPositions.isEmpty()) {
                return;
            }
            float x = textPositions.stream().map(org.apache.pdfbox.text.TextPosition::getXDirAdj)
                    .min(Float::compare).orElse(0f);
            float endX = (float) textPositions.stream()
                    .mapToDouble(position -> position.getXDirAdj() + position.getWidthDirAdj())
                    .max().orElse(x);
            float y = textPositions.stream().map(org.apache.pdfbox.text.TextPosition::getYDirAdj)
                    .min(Float::compare).orElse(0f);
            runs.add(new TextRun(text, x, endX, y));
        }

        private List<TextRun> runs() {
            return List.copyOf(runs);
        }
    }

    private record TextRun(String text, float x, float endX, float y) {
    }

    private record RenderedLine(String text, float minimumX) {
    }
}
