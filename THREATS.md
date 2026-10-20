# TrustVault Threat Model

## 1. Security Objective

TrustVault is a secure document-sharing application. The main security rule is:

> Knowing a document ID is never enough to access a document.

The backend must verify the authenticated user and the latest database permissions for every protected document operation.

TrustVault follows a deny-by-default approach.

---

## 2. Assets to Protect

TrustVault protects:

- User accounts and credentials
- JWT authentication tokens
- Uploaded private documents
- Document metadata
- Document ownership information
- READ and EDIT permissions
- Audit logs
- Administrative activity information

---

## 3. Trust Boundaries

The main trust boundaries are:

1. Browser to React frontend
2. React frontend to Spring Boot REST API
3. Spring Boot API to PostgreSQL
4. Spring Boot API to private file storage

The frontend is not trusted to make authorization decisions.

All important authorization checks are performed by the backend.

---

## 4. Threat: IDOR / Broken Object Level Authorization

### Risk

An attacker may obtain or guess another document's UUID and directly call a download, edit, delete, or permission endpoint.

### Mitigation

The backend checks the authenticated user's identity against the document owner and current database permissions.

A document ID alone never grants access.

Unauthorized attempts are refused and recorded in the audit log.

---

## 5. Threat: Unauthorized Document Download

### Risk

A user may attempt to download another user's private document.

### Mitigation

Before reading the stored file, the backend verifies that the requester is:

- the document owner, or
- a user with current READ permission, or
- a user with current EDIT permission.

Otherwise the request is denied.

Automated tests verify that one user cannot download another user's private document.

---

## 6. Threat: Unauthorized Edit or Delete

### Risk

A user may directly call document modification endpoints.

### Mitigation

EDIT permission is required for editing a shared document.

Only the document owner can delete the document.

These rules are enforced by the backend rather than by hiding frontend buttons.

---

## 7. Threat: Unauthorized Permission Changes

### Risk

A viewer may attempt to grant themselves or another user access.

### Mitigation

Only the current document owner can grant or revoke permissions.

The backend verifies ownership before modifying the permission table.

Unauthorized permission-change attempts are denied and audited.

---

## 8. Threat: Stale Access After Revocation

### Risk

A user may keep an old JWT after the owner revokes document access.

### Mitigation

JWTs are used to establish identity, not document authorization.

The current permission is read from the database for every protected document operation.

Therefore revocation takes effect immediately even when the user still has an unexpired JWT.

---

## 9. Threat: Disabled User With Existing JWT

### Risk

A disabled account may still possess a previously issued valid JWT.

### Mitigation

Protected document and permission operations verify the current user record in the database.

If the account is disabled, access is refused even when the JWT has not yet expired.

---

## 10. Threat: Privilege Escalation During Registration

### Risk

A malicious user may attempt to register themselves as ADMIN or another privileged role.

### Mitigation

Public registration does not accept a trusted role from the client.

New accounts are assigned the VIEWER role by the backend.

The client cannot self-assign ADMIN privileges.

---

## 11. Threat: JWT Manipulation

### Risk

An attacker may modify JWT claims such as email or role.

### Mitigation

JWTs are signed using an HMAC secret.

The backend validates the signature, expiration, and expected issuer.

The JWT secret is provided through an environment variable and is not stored in source code.

---

## 12. Threat: Hardcoded Secrets

### Risk

Database passwords or JWT signing secrets committed to a public Git repository could expose the system.

### Mitigation

TrustVault uses environment variables for sensitive configuration.

The `.gitignore` excludes environment files and other secret/local configuration files.

Production secrets must be configured through the deployment platform.

---

## 13. Threat: Direct File Storage Access

### Risk

Private documents could become accessible through predictable public URLs.

### Mitigation

Uploaded files are stored in a private storage directory and are not exposed as static web resources.

The application generates a random storage key instead of using the original filename as the physical storage name.

Files are served only after backend authorization succeeds.

---

## 14. Threat: Path Traversal

### Risk

A malicious filename such as `../../secret.txt` may attempt to escape the intended storage directory.

### Mitigation

Upload validation rejects unsafe filename patterns.

Generated storage keys are used for physical files.

Resolved paths are normalized and checked to ensure they remain inside the configured private storage directory.

---

## 15. Threat: Malicious or Invalid Upload

### Risk

Attackers may upload oversized or unsupported files.

### Mitigation

TrustVault validates uploads before storing them.

Current controls include:

- empty-file rejection
- maximum file size
- allowed content types
- unsafe filename rejection

### Remaining Limitation

Content type is currently validated using upload metadata. A stronger production implementation should additionally inspect file signatures/magic bytes and perform malware scanning.

---

## 16. Threat: Metadata Leakage Through Search

### Risk

Search results could reveal filenames, owners, categories, or dates belonging to documents the requester cannot access.

### Mitigation

Search results are filtered using the authenticated user's ownership and current document permissions before being returned.

Unauthorized document metadata is not intentionally exposed.

---

## 17. Threat: Audit Log Tampering

### Risk

An attacker may attempt to modify or remove evidence of unauthorized activity.

### Mitigation

Ordinary users do not receive endpoints for modifying audit records.

Audit entries contain a hash linked to the previous audit entry, making silent modification easier to detect.

Denied access attempts are logged using a separate transaction so the denial record is not lost when the main operation fails.

### Remaining Limitation

The current hash chain is application-level protection. An attacker with complete control over both the application and database could potentially rewrite the chain.

A production system should use external append-only logging or independently anchored hashes.

---

## 18. Threat: Concurrent Audit Writes

### Risk

Concurrent requests could interfere with hash-chain ordering.

### Mitigation

The current application serializes audit creation within a single application instance.

### Remaining Limitation

This does not provide distributed locking when multiple backend instances are deployed.

A production deployment should use database locking, sequence-based ordering, or an external append-only audit service.

---

## 19. Threat: Frontend Authorization Bypass

### Risk

A user can modify browser JavaScript, localStorage, or manually call API endpoints.

### Mitigation

Frontend controls are only for user experience.

The backend independently authenticates and authorizes every sensitive operation.

Changing a frontend role value or revealing a hidden button does not grant backend access.

---

## 20. Security Testing

TrustVault includes automated authorization testing using a separate in-memory H2 test database.

The security test verifies that one authenticated user cannot download another user's private document.

The test configuration also verifies that automated tests are connected to H2 instead of the development PostgreSQL database, reducing the risk of destructive test operations affecting real development data.

---

## 21. Known Limitations and Future Hardening

For a production-grade deployment, the following improvements are recommended:

- file signature / magic-byte validation
- malware scanning
- external object storage with private access controls
- distributed audit-log integrity protection
- rate limiting for authentication and sensitive endpoints
- stronger monitoring and alerting
- secure secret-management service
- additional integration tests for every permission transition
- transactional coordination between database deletion and file deletion

---

## 22. Core Security Invariants

TrustVault is designed around these rules:

1. Authentication does not automatically grant document access.
2. Knowing a document UUID is never sufficient for access.
3. The backend makes every authorization decision.
4. Document permissions are checked from current database state.
5. Revocation takes effect immediately.
6. Only owners can manage document permissions.
7. Unauthorized attempts are refused and audited.
8. Frontend controls are never treated as security controls.
9. Secrets must not be committed to source control.
10. Automated security tests must not use the development database.