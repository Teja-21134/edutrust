package com.edutrust.database;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_families")
public class DocumentFamily {

    @Id
    private UUID id;

    @Column(name = "institution_key", nullable = false, length = 100)
    private String institutionKey;

    @Column(name = "family_key", nullable = false, length = 150)
    private String familyKey;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected DocumentFamily() {
    }

    public DocumentFamily(UUID id, String institutionKey, String familyKey, String displayName) {
        this.id = id;
        this.institutionKey = institutionKey;
        this.familyKey = familyKey;
        this.displayName = displayName;
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

    public String getInstitutionKey() {
        return institutionKey;
    }

    public String getFamilyKey() {
        return familyKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
