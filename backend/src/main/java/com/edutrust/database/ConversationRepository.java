package com.edutrust.database;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByOwner_IdOrderByUpdatedAtDesc(UUID ownerId);

    Optional<Conversation> findByIdAndOwner_Id(UUID id, UUID ownerId);
}
