package com.edutrust.ingestion;

/** A page-local piece of cleaned document text. */
public record Chunk(int chunkIndex, int pageNumber, String text) {
}
