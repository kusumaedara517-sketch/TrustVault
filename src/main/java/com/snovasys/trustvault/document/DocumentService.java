package com.snovasys.trustvault.document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.snovasys.trustvault.audit.AuditEvent;
import com.snovasys.trustvault.audit.AuditService;
import com.snovasys.trustvault.user.User;
import com.snovasys.trustvault.user.UserRepository;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final FileValidationService fileValidationService;

    private final Path storageLocation =
            Paths.get("private-storage").toAbsolutePath().normalize();

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentPermissionRepository permissionRepository,
            UserRepository userRepository,
            AuditService auditService,
            FileValidationService fileValidationService)
            throws IOException {

        this.documentRepository = documentRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.fileValidationService = fileValidationService;

        Files.createDirectories(storageLocation);
    }

    private User getActiveUser(String userEmail) {

        User user = userRepository
                .findByEmail(userEmail.toLowerCase())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (!user.isEnabled()) {

            auditService.log(
                    userEmail,
                    AuditEvent.ACCESS_DENIED,
                    null,
                    "Disabled user access refused",
                    false
            );

            throw new SecurityException(
                    "Your account is disabled"
            );
        }

        return user;
    }

    public Document upload(
            MultipartFile file,
            String category,
            String userEmail) throws IOException {

        User owner = getActiveUser(userEmail);

        fileValidationService.validate(file);

        String originalFilename = file.getOriginalFilename();
        String storageKey = UUID.randomUUID().toString();

        Path target =
                storageLocation.resolve(storageKey).normalize();

        if (!target.startsWith(storageLocation)) {
            throw new SecurityException("Invalid file path");
        }

        Files.copy(
                file.getInputStream(),
                target,
                StandardCopyOption.REPLACE_EXISTING
        );

        Document document = new Document();
        document.setOwner(owner);
        document.setOriginalFilename(originalFilename);
        document.setStorageKey(storageKey);

        document.setCategory(
                category == null || category.isBlank()
                        ? "General"
                        : category
        );

        document.setContentType(
                file.getContentType() == null
                        ? "application/octet-stream"
                        : file.getContentType()
        );

        document.setFileSize(file.getSize());

        Document savedDocument =
                documentRepository.save(document);

        auditService.log(
                userEmail,
                AuditEvent.DOCUMENT_UPLOAD,
                savedDocument.getId(),
                "Uploaded: " + originalFilename,
                true
        );

        return savedDocument;
    }

    public List<Document> searchDocuments(
            String name,
            String owner,
            String category,
            LocalDate date,
            String userEmail) {

        User user = getActiveUser(userEmail);

        List<Document> documents;

        if (name != null && !name.isBlank()) {

            documents =
                    documentRepository
                            .findByOriginalFilenameContainingIgnoreCase(name);

        } else if (owner != null && !owner.isBlank()) {

            documents =
                    documentRepository
                            .findByOwnerEmailContainingIgnoreCase(owner);

        } else if (category != null && !category.isBlank()) {

            documents =
                    documentRepository
                            .findByCategoryContainingIgnoreCase(category);

        } else if (date != null) {

            LocalDateTime start =
                    date.atStartOfDay();

            LocalDateTime end =
                    date.plusDays(1).atStartOfDay();

            documents =
                    documentRepository
                            .findByCreatedAtBetween(start, end);

        } else {

            documents = documentRepository.findAll();
        }

        return documents.stream()
                .filter(document -> {

                    boolean isOwner =
                            document.getOwner()
                                    .getId()
                                    .equals(user.getId());

                    if (isOwner) {
                        return true;
                    }

                    return permissionRepository
                            .findByDocumentIdAndUserId(
                                    document.getId(),
                                    user.getId()
                            )
                            .isPresent();
                })
                .toList();
    }

    public String getPermissionForDocument(
            UUID documentId,
            String userEmail) {

        User user = getActiveUser(userEmail);

        Document document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Document not found"
                                ));

        boolean isOwner =
                document.getOwner()
                        .getId()
                        .equals(user.getId());

        if (isOwner) {
            return "OWNER";
        }

        return permissionRepository
                .findByDocumentIdAndUserId(
                        documentId,
                        user.getId()
                )
                .map(permission ->
                        permission.getPermission().name()
                )
                .orElse("NONE");
    }

    public DocumentDownload download(
            UUID documentId,
            String userEmail) throws IOException {

        User user = getActiveUser(userEmail);

        Document document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Document not found"
                                ));

        boolean isOwner =
                document.getOwner()
                        .getId()
                        .equals(user.getId());

        boolean hasPermission =
                permissionRepository
                        .findByDocumentIdAndUserId(
                                documentId,
                                user.getId()
                        )
                        .map(permission ->
                                permission.getPermission()
                                        == PermissionType.READ
                                        ||
                                permission.getPermission()
                                        == PermissionType.EDIT
                        )
                        .orElse(false);

        if (!isOwner && !hasPermission) {

            auditService.log(
                    userEmail,
                    AuditEvent.ACCESS_DENIED,
                    documentId,
                    "Download refused",
                    false
            );

            throw new SecurityException(
                    "You do not have permission to access this document"
            );
        }

        Path filePath =
                storageLocation
                        .resolve(document.getStorageKey())
                        .normalize();

        if (!filePath.startsWith(storageLocation)) {
            throw new SecurityException("Invalid file path");
        }

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException(
                    "Stored file not found"
            );
        }

        byte[] data =
                Files.readAllBytes(filePath);

        auditService.log(
                userEmail,
                AuditEvent.DOCUMENT_DOWNLOAD,
                documentId,
                "Downloaded: "
                        + document.getOriginalFilename(),
                true
        );

        return new DocumentDownload(
                data,
                document.getOriginalFilename(),
                document.getContentType()
        );
    }

    public Document updateDocument(
            UUID documentId,
            String category,
            String userEmail) {

        User user = getActiveUser(userEmail);

        Document document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Document not found"
                                ));

        boolean isOwner =
                document.getOwner()
                        .getId()
                        .equals(user.getId());

        boolean canEdit =
                permissionRepository
                        .findByDocumentIdAndUserId(
                                documentId,
                                user.getId()
                        )
                        .map(permission ->
                                permission.getPermission()
                                        == PermissionType.EDIT
                        )
                        .orElse(false);

        if (!isOwner && !canEdit) {

            auditService.log(
                    userEmail,
                    AuditEvent.ACCESS_DENIED,
                    documentId,
                    "Edit refused",
                    false
            );

            throw new SecurityException(
                    "You do not have permission to edit this document"
            );
        }

        if (category != null && !category.isBlank()) {
            document.setCategory(category);
        }

        Document savedDocument =
                documentRepository.save(document);

        auditService.log(
                userEmail,
                AuditEvent.DOCUMENT_EDIT,
                documentId,
                "Document updated",
                true
        );

        return savedDocument;
    }

    public void deleteDocument(
            UUID documentId,
            String userEmail) throws IOException {

        User user = getActiveUser(userEmail);

        Document document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Document not found"
                                ));

        if (!document.getOwner()
                .getId()
                .equals(user.getId())) {

            auditService.log(
                    userEmail,
                    AuditEvent.ACCESS_DENIED,
                    documentId,
                    "Delete refused",
                    false
            );

            throw new SecurityException(
                    "Only document owner can delete this document"
            );
        }

        Path filePath =
                storageLocation
                        .resolve(document.getStorageKey())
                        .normalize();

        if (!filePath.startsWith(storageLocation)) {
            throw new SecurityException(
                    "Invalid file path"
            );
        }

        List<DocumentPermission> permissions =
                permissionRepository
                        .findAll()
                        .stream()
                        .filter(permission ->
                                permission
                                        .getDocument()
                                        .getId()
                                        .equals(documentId))
                        .toList();

        permissionRepository.deleteAll(permissions);

        auditService.log(
                userEmail,
                AuditEvent.DOCUMENT_DELETE,
                documentId,
                "Deleted: "
                        + document.getOriginalFilename(),
                true
        );

        documentRepository.delete(document);

        Files.deleteIfExists(filePath);
    }

    public static class DocumentDownload {

        private final byte[] data;
        private final String filename;
        private final String contentType;

        public DocumentDownload(
                byte[] data,
                String filename,
                String contentType) {

            this.data = data;
            this.filename = filename;
            this.contentType = contentType;
        }

        public byte[] getData() {
            return data;
        }

        public String getFilename() {
            return filename;
        }

        public String getContentType() {
            return contentType;
        }
    }
}