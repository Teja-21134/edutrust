package com.edutrust.database;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentFamilyRepository extends JpaRepository<DocumentFamily, UUID> {

    Optional<DocumentFamily> findByInstitutionKeyAndFamilyKey(String institutionKey, String familyKey);
}
