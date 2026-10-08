package com.edutrust.database;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findByDocumentHash(String documentHash);
}
