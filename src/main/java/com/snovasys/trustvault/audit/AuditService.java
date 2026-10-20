package com.snovasys.trustvault.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized void log(
            String userEmail,
            AuditEvent event,
            UUID documentId,
            String details,
            boolean success) {

        try {
            String previousHash = auditLogRepository
                    .findTopByOrderByTimestampDesc()
                    .map(AuditLog::getCurrentHash)
                    .orElse("GENESIS");

            LocalDateTime timestamp = LocalDateTime.now();

            String data =
                    previousHash + "|" +
                    timestamp + "|" +
                    userEmail + "|" +
                    event.name() + "|" +
                    documentId + "|" +
                    details + "|" +
                    success;

            String currentHash = calculateHash(data);

            AuditLog auditLog = new AuditLog();

            auditLog.setTimestamp(timestamp);
            auditLog.setUserEmail(
                    userEmail == null ? "UNKNOWN" : userEmail
            );
            auditLog.setEvent(event);
            auditLog.setDocumentId(documentId);
            auditLog.setDetails(
                    details == null ? "" : details
            );
            auditLog.setSuccess(success);
            auditLog.setPreviousHash(previousHash);
            auditLog.setCurrentHash(currentHash);

            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not create audit log",
                    e
            );
        }
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository
                .findAllByOrderByTimestampDesc();
    }

    private String calculateHash(String data) throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

        byte[] hash = digest.digest(
                data.getBytes(StandardCharsets.UTF_8)
        );

        return HexFormat.of().formatHex(hash);
    }
}