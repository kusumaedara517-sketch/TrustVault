package com.snovasys.trustvault.document;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.snovasys.trustvault.audit.AuditEvent;
import com.snovasys.trustvault.audit.AuditService;
import com.snovasys.trustvault.user.User;
import com.snovasys.trustvault.user.UserRepository;

@Service
public class PermissionService {

    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public PermissionService(
            DocumentRepository documentRepository,
            DocumentPermissionRepository permissionRepository,
            UserRepository userRepository,
            AuditService auditService) {

        this.documentRepository = documentRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private User getActiveUser(String email) {

        User user = userRepository
                .findByEmail(email.toLowerCase())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        ));

        if (!user.isEnabled()) {

            auditService.log(
                    email,
                    AuditEvent.ACCESS_DENIED,
                    null,
                    "Disabled user permission action refused",
                    false
            );

            throw new SecurityException(
                    "Your account is disabled"
            );
        }

        return user;
    }

    @Transactional
    public void grantPermission(
            UUID documentId,
            String targetEmail,
            PermissionType permissionType,
            String ownerEmail) {

        User owner = getActiveUser(ownerEmail);

        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Document not found"
                        ));

        if (!document.getOwner()
                .getId()
                .equals(owner.getId())) {

            auditService.log(
                    ownerEmail,
                    AuditEvent.ACCESS_DENIED,
                    documentId,
                    "Permission grant refused",
                    false
            );

            throw new SecurityException(
                    "Only document owner can grant access"
            );
        }

        User targetUser = userRepository
                .findByEmail(targetEmail.toLowerCase())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Target user not found"
                        ));

        if (!targetUser.isEnabled()) {
            throw new IllegalArgumentException(
                    "Cannot grant access to a disabled user"
            );
        }

        if (targetUser.getId()
                .equals(owner.getId())) {

            throw new IllegalArgumentException(
                    "Owner already has full access"
            );
        }

        DocumentPermission documentPermission =
                permissionRepository
                        .findByDocumentIdAndUserId(
                                documentId,
                                targetUser.getId()
                        )
                        .orElse(new DocumentPermission());

        documentPermission.setDocument(document);
        documentPermission.setUser(targetUser);
        documentPermission.setPermission(permissionType);

        permissionRepository.save(documentPermission);

        auditService.log(
                ownerEmail,
                AuditEvent.ACCESS_GRANTED,
                documentId,
                "Granted " + permissionType
                        + " to " + targetUser.getEmail(),
                true
        );
    }

    @Transactional
    public void revokePermission(
            UUID documentId,
            String targetEmail,
            String ownerEmail) {

        User owner = getActiveUser(ownerEmail);

        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Document not found"
                        ));

        if (!document.getOwner()
                .getId()
                .equals(owner.getId())) {

            auditService.log(
                    ownerEmail,
                    AuditEvent.ACCESS_DENIED,
                    documentId,
                    "Permission revoke refused",
                    false
            );

            throw new SecurityException(
                    "Only document owner can revoke access"
            );
        }

        User targetUser = userRepository
                .findByEmail(targetEmail.toLowerCase())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Target user not found"
                        ));

        permissionRepository
                .deleteByDocumentIdAndUserId(
                        documentId,
                        targetUser.getId()
                );

        auditService.log(
                ownerEmail,
                AuditEvent.ACCESS_REVOKED,
                documentId,
                "Revoked access from "
                        + targetUser.getEmail(),
                true
        );
    }
}