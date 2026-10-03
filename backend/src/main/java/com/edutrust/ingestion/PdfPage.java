package com.edutrust.ingestion;

/** Text extracted from one PDF page. Page numbers are one-based. */
public record PdfPage(int pageNumber, String text) {
}
