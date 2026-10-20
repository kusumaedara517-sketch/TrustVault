package com.snovasys.trustvault.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snovasys.trustvault.audit.AuditEvent;
import com.snovasys.trustvault.audit.AuditService;
import com.snovasys.trustvault.security.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {

        String email = request.getEmail().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity
                    .badRequest()
                    .body("Email already registered");
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(email);

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        // Public registration can never create ADMIN.
        user.setRole(Role.VIEWER);

        userRepository.save(user);

        return ResponseEntity.ok(
                "User registered successfully"
        );
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        String email =
                request.getEmail().toLowerCase();

        User user = userRepository
                .findByEmail(email)
                .orElse(null);

        if (user == null ||
                !passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash())) {

            auditService.log(
                    email,
                    AuditEvent.LOGIN_FAILED,
                    null,
                    "Invalid email or password",
                    false
            );

            return ResponseEntity
                    .status(401)
                    .body("Invalid email or password");
        }

        if (!user.isEnabled()) {

            auditService.log(
                    email,
                    AuditEvent.LOGIN_FAILED,
                    null,
                    "Account disabled",
                    false
            );

            return ResponseEntity
                    .status(403)
                    .body("Account is disabled");
        }

        String token =
                jwtService.generateToken(
                        user.getEmail(),
                        user.getRole().name()
                );

        auditService.log(
                email,
                AuditEvent.LOGIN_SUCCESS,
                null,
                "Login successful",
                true
        );

        return ResponseEntity.ok(
                new LoginResponse(
                        "Login successful",
                        user.getEmail(),
                        user.getRole(),
                        token
                )
        );
    }
}