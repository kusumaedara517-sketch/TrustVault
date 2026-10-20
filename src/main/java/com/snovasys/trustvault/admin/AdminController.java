package com.snovasys.trustvault.admin;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snovasys.trustvault.audit.AuditLog;
import com.snovasys.trustvault.audit.AuditService;
import com.snovasys.trustvault.document.DocumentPermission;
import com.snovasys.trustvault.document.DocumentPermissionRepository;
import com.snovasys.trustvault.user.Role;
import com.snovasys.trustvault.user.User;
import com.snovasys.trustvault.user.UserRepository;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final DocumentPermissionRepository permissionRepository;

    public AdminController(
            UserRepository userRepository,
            AuditService auditService,
            DocumentPermissionRepository permissionRepository) {

        this.userRepository = userRepository;
        this.auditService = auditService;
        this.permissionRepository = permissionRepository;
    }

    private User getAdmin(Authentication authentication) {

        User user = userRepository
                .findByEmail(authentication.getName().toLowerCase())
                .orElseThrow(() ->
                        new SecurityException("User not found"));

        if (!user.isEnabled() || user.getRole() != Role.ADMIN) {
            throw new SecurityException("Admin access required");
        }

        return user;
    }

    @GetMapping("/audit")
    public ResponseEntity<?> getAuditLogs(
            Authentication authentication) {

        try {
            getAdmin(authentication);

            List<AuditLog> logs =
                    auditService.getAllLogs();

            return ResponseEntity.ok(logs);

        } catch (SecurityException e) {

            return ResponseEntity
                    .status(403)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/permissions")
    public ResponseEntity<?> getPermissions(
            Authentication authentication) {

        try {
            getAdmin(authentication);

            List<Map<String, Object>> response =
                    permissionRepository
                            .findAll()
                            .stream()
                            .map(permission ->
                                    createPermissionResponse(permission))
                            .toList();

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {

            return ResponseEntity
                    .status(403)
                    .body(e.getMessage());
        }
    }

    private Map<String, Object> createPermissionResponse(
            DocumentPermission permission) {

        return Map.of(
                "documentId",
                permission.getDocument().getId(),

                "documentName",
                permission.getDocument().getOriginalFilename(),

                "owner",
                permission.getDocument().getOwner().getEmail(),

                "sharedWith",
                permission.getUser().getEmail(),

                "permission",
                permission.getPermission().name()
        );
    }
}