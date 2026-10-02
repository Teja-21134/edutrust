package com.edutrust.database;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chunks")
public class Chunk {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Chunk() {
    }

    public Chunk(UUID id, Document document, int pageNumber, int chunkIndex, String text) {
        this.id = id;
        this.document = document;
        this.pageNumber = pageNumber;
        this.chunkIndex = chunkIndex;
        this.text = text;
    }

    @PrePersist
    void setCreatedAtIfMissing() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
