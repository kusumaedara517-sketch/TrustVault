package com.snovasys.trustvault.document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentPermissionRepository
        extends JpaRepository<DocumentPermission, UUID> {

    Optional<DocumentPermission> findByDocumentIdAndUserId(
            UUID documentId,
            UUID userId
    );

    void deleteByDocumentIdAndUserId(
            UUID documentId,
            UUID userId
    );

    List<DocumentPermission> findByDocumentId(
            UUID documentId
    );
}