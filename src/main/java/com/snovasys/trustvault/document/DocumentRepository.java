package com.snovasys.trustvault.document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByOwnerId(UUID ownerId);

    List<Document> findByOriginalFilenameContainingIgnoreCase(String filename);

    List<Document> findByCategoryContainingIgnoreCase(String category);

    List<Document> findByOwnerEmailContainingIgnoreCase(String email);

    List<Document> findByCreatedAtBetween(
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}