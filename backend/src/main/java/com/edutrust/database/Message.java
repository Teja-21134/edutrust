package com.edutrust.database;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> sources;

    @Column(name = "faithfulness_status")
    private String faithfulnessStatus;

    @Column(name = "faithfulness_score")
    private Double faithfulnessScore;

    @Column(name = "conflict_detected")
    private Boolean conflictDetected;

    @Column(name = "conflict_resolution")
    private String conflictResolution;

    @Column(name = "selected_document")
    private String selectedDocument;

    @Column(name = "selected_version")
    private String selectedVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Message() {
    }

    public Message(UUID id, MessageRole role, String content) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    @jakarta.persistence.PrePersist
    void setCreatedAtIfMissing() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    void setConversation(Conversation conversation) { this.conversation = conversation; }
    public UUID getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public MessageRole getRole() { return role; }
    public String getContent() { return content; }
    public List<Map<String, Object>> getSources() { return sources; }
    public String getFaithfulnessStatus() { return faithfulnessStatus; }
    public Double getFaithfulnessScore() { return faithfulnessScore; }
    public Boolean getConflictDetected() { return conflictDetected; }
    public String getConflictResolution() { return conflictResolution; }
    public String getSelectedDocument() { return selectedDocument; }
    public String getSelectedVersion() { return selectedVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setAssistantMetadata(
            List<Map<String, Object>> sources,
            String faithfulnessStatus,
            double faithfulnessScore,
            boolean conflictDetected,
            String conflictResolution,
            String selectedDocument,
            String selectedVersion) {
        this.sources = sources;
        this.faithfulnessStatus = faithfulnessStatus;
        this.faithfulnessScore = faithfulnessScore;
        this.conflictDetected = conflictDetected;
        this.conflictResolution = conflictResolution;
        this.selectedDocument = selectedDocument;
        this.selectedVersion = selectedVersion;
    }
}
