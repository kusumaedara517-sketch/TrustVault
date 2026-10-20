package com.snovasys.trustvault.document;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping("/{documentId}/permissions")
    public ResponseEntity<?> grantPermission(
            @PathVariable UUID documentId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String targetEmail = request.get("email");
            String permission = request.get("permission");

            if (targetEmail == null || permission == null) {
                return ResponseEntity.badRequest()
                        .body("Email and permission are required");
            }

            PermissionType permissionType =
                    PermissionType.valueOf(permission.toUpperCase());

            permissionService.grantPermission(
                    documentId,
                    targetEmail,
                    permissionType,
                    authentication.getName()
            );

            return ResponseEntity.ok("Permission granted successfully");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @DeleteMapping("/{documentId}/permissions")
    public ResponseEntity<?> revokePermission(
            @PathVariable UUID documentId,
            @RequestParam String email,
            Authentication authentication) {

        try {
            permissionService.revokePermission(
                    documentId,
                    email,
                    authentication.getName()
            );

            return ResponseEntity.ok("Permission revoked successfully");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
}