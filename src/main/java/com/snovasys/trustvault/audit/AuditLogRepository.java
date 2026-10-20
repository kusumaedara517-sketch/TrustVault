package com.snovasys.trustvault.audit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, UUID> {

    Optional<AuditLog> findTopByOrderByTimestampDesc();

    List<AuditLog> findAllByOrderByTimestampDesc();
}