package com.snovasys.trustvault.document;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            Authentication authentication) {

        try {
            Document document = documentService.upload(
                    file,
                    category,
                    authentication.getName()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document uploaded successfully");
            response.put("documentId", document.getId());
            response.put("filename", document.getOriginalFilename());
            response.put("category", document.getCategory());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Could not store document");
        }
    }

    @GetMapping
    public ResponseEntity<?> searchDocuments(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) LocalDate date,
            Authentication authentication) {

        try {
            List<Document> documents =
                    documentService.searchDocuments(
                            name,
                            owner,
                            category,
                            date,
                            authentication.getName()
                    );

            List<Map<String, Object>> response =
                    documents.stream()
                            .map(document -> {

                                Map<String, Object> item =
                                        new HashMap<>();

                                item.put(
                                        "id",
                                        document.getId()
                                );

                                item.put(
                                        "filename",
                                        document.getOriginalFilename()
                                );

                                item.put(
                                        "category",
                                        document.getCategory()
                                );

                                item.put(
                                        "owner",
                                        document.getOwner().getEmail()
                                );

                                item.put(
                                        "fileSize",
                                        document.getFileSize()
                                );

                                item.put(
                                        "createdAt",
                                        document.getCreatedAt()
                                );

                                return item;
                            })
                            .toList();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<?> downloadDocument(
            @PathVariable UUID documentId,
            Authentication authentication) {

        try {
            DocumentService.DocumentDownload download =
                    documentService.download(
                            documentId,
                            authentication.getName()
                    );

            String safeFilename =
                    download.getFilename()
                            .replace("\"", "")
                            .replace("\r", "")
                            .replace("\n", "");

            return ResponseEntity.ok()
                    .contentType(
                            MediaType.parseMediaType(
                                    download.getContentType()
                            )
                    )
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" +
                                    safeFilename +
                                    "\""
                    )
                    .body(download.getData());

        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                    .body(e.getMessage());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Could not read document");
        }
    }

    @PutMapping("/{documentId}")
    public ResponseEntity<?> updateDocument(
            @PathVariable UUID documentId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String category = request.get("category");

            Document document =
                    documentService.updateDocument(
                            documentId,
                            category,
                            authentication.getName()
                    );

            Map<String, Object> response = new HashMap<>();

            response.put(
                    "message",
                    "Document updated successfully"
            );

            response.put(
                    "documentId",
                    document.getId()
            );

            response.put(
                    "category",
                    document.getCategory()
            );

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                    .body(e.getMessage());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable UUID documentId,
            Authentication authentication) {

        try {
            documentService.deleteDocument(
                    documentId,
                    authentication.getName()
            );

            return ResponseEntity.ok(
                    "Document deleted successfully"
            );

        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                    .body(e.getMessage());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Could not delete document");
        }
    }
}