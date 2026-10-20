# TrustVault AI Prompt & Review Log

This document records how AI assistance was used while building TrustVault, what suggestions were produced, what problems were found, and how the final implementation was corrected.

AI-generated suggestions were reviewed and tested before being accepted. Security decisions were not delegated blindly to AI.

---

## Entry 1 — 2026-10-19 — Lombok Build Failure

### Prompt / Goal

Reduce boilerplate code in Java entity classes and configure the Spring Boot project correctly.

### AI Output / Suggestion

AI initially suggested using Lombok annotations to generate getters, setters, and constructors.

### What Was Wrong

The project environment was running Java 25 while Maven targeted Java 21. The Lombok annotation-processing setup caused compilation/toolchain problems.

Depending on Lombok was unnecessary for the small project and made the build less predictable.

### Fix

Lombok was removed.

Explicit constructors, getters, and setters were written in the Java classes.

### Verification

The Maven project compiled successfully after removing the Lombok dependency and replacing generated methods with normal Java methods.

### Commit

https://github.com/kusumaedara517-sketch/TrustVault/commit/b43024b

---

## Entry 2 — 2026-10-19 — JWT Resource Server / IDE Diagnostics

### Prompt / Goal

Implement JWT authentication for TrustVault using Spring Security.

### AI Output / Suggestion

AI suggested Spring Security OAuth2 Resource Server classes for validating bearer JWTs.

### What Was Wrong

After Maven configuration changes, VS Code temporarily reported that JWT/resource-server packages were missing even though Maven dependencies were correctly configured.

Treating the IDE warning as a real Maven dependency failure would have caused unnecessary code changes.

### Fix

The Maven build was used as the source of truth.

The Java language server workspace was refreshed instead of changing working dependencies.

### Verification

Maven compiled the project successfully and JWT authentication worked during login and protected API testing.

### Commit

https://github.com/kusumaedara517-sketch/TrustVault/commit/b43024b

---

## Entry 3 — 2026-10-19 — Authentication DTO Mismatch

### Prompt / Goal

Return the JWT token, user email, role, and login message from the authentication endpoint.

### AI Output / Suggestion

AI updated the authentication controller to construct a LoginResponse containing the additional JWT information.

### What Was Wrong

The corresponding LoginResponse class had not been updated/saved consistently, which caused a constructor mismatch during compilation.

### Fix

The complete LoginResponse DTO was replaced so its fields and constructor matched the AuthController response.

### Verification

Maven compilation succeeded and the login endpoint returned the JWT and user information correctly.

### Commit

https://github.com/kusumaedara517-sketch/TrustVault/commit/b43024b

---

## Entry 4 — 2026-10-19 — Authorization and Immediate Revocation

### Prompt / Goal

Ensure that a user cannot access another user's document unless the owner explicitly grants permission, and ensure revocation works immediately.

### AI Output / Suggestion

AI suggested using JWT for authentication while storing document READ/EDIT permissions in PostgreSQL.

The backend checks the current permission record for each protected document request instead of placing document permissions inside the JWT.

### Security Review

This design was accepted because embedding document grants inside a long-lived JWT could allow stale access after an owner revoked permission.

### Fix / Final Design

JWT proves the identity of the requester.

Document authorization is determined from the latest database state for every protected operation.

### Verification

A viewer was granted READ access and successfully downloaded the shared document.

The owner then revoked access.

Using the viewer's existing unexpired JWT after revocation resulted in HTTP 403.

This demonstrated immediate revocation.

### Commit

https://github.com/kusumaedara517-sketch/TrustVault/commit/b43024b

---

## Entry 5 — 2026-10-19 — Unauthorized Permission Change

### Prompt / Goal

Verify that a viewer cannot grant themselves or another user EDIT access.

### AI Output / Suggestion

AI suggested owner-only authorization inside PermissionService rather than relying on whether the frontend displays the Share button.

### Security Review

This follows the TrustVault rule that frontend controls are not security controls.

### Fix / Final Design

The backend retrieves the document and verifies that the authenticated user is the actual document owner before granting or revoking permissions.

Unauthorized permission changes generate an ACCESS_DENIED audit event.

### Verification

A non-owner attempted to grant EDIT permission and the backend refused the operation.

The refused attempt appeared in the audit trail.

### Commit

https://github.com/kusumaedara517-sketch/TrustVault/commit/b43024b

---

## Entry 6 — 2026-10-19 — Incorrect Java File Placement

### Prompt / Goal

Add authentication and audit-related backend files.

### AI Output / Suggestion

During step-by-step file creation guidance, instructions were not sufficiently clear about the required Java package location.

### What Was Wrong

AuthController was accidentally placed under the audit package/folder, causing a Java package/file-location problem.

### Fix

The controller was moved back to the correct user package and its package declaration was verified.

### Lesson

For subsequent changes, complete file paths and complete replacement code were used instead of ambiguous partial instructions.

### Verification

The backend compiled successfully after correcting the file location.

### Commit

https://github.com/kusumaedara517-sketch/TrustVault/commit/b43024b

---

## Entry 7 — 2026-10-19 — Security Test File Location

### Prompt / Goal

Create an automated test proving that one user cannot access another user's private document.

### AI Output / Suggestion

AI provided the authorization test but the initial file-location workflow caused confusion between main and test source directories.

### What Was Wrong

The test was initially created in the wrong source/package location.

### Fix

The test was placed at:

`src/test/java/com/snovasys/trustvault/DocumentSecurityTest.java`

### Verification

Maven discovered and executed the test successfully.

### Commit

https://github.com/kusumaedara517-sketch/TrustVault/commit/b43024b

---

## Entry 8 — 2026-10-19 — PowerShell npm Execution Issue

### Prompt / Goal

Start the React/Vite frontend.

### AI Output / Suggestion

The normal npm command was initially expected to run from PowerShell.

### What Was Wrong

PowerShell execution policy blocked `npm.ps1`.

Changing the machine's execution policy would have been an unnecessary system-level workaround.

### Fix

The Windows command wrapper `npm.cmd` was used instead.

### Verification

The Vite React development server started successfully on localhost.

### Commit

https://github.com/kusumaedara517-sketch/TrustVault/commit/b43024b

---

## Entry 9 — 2026-10-20 — Port 8080 Conflict

### Prompt / Goal

Restart the backend after security configuration changes.

### AI Output / Suggestion

AI initially instructed another Spring Boot start while an older backend process was still listening on port 8080.

### What Was Wrong

The new application failed with:

`Web server failed to start. Port 8080 was already in use.`

Repeated application starts were unnecessary.

### Fix

The listening process was identified using:

`netstat -ano | findstr :8080`

Only the old backend PID was terminated before starting the updated application.

### Verification

Spring Boot subsequently started successfully on port 8080.

### Commit

https://github.com/kusumaedara517-sketch/TrustVault/commit/b43024b

---

## Entry 10 — 2026-10-20 — JWT Secret Generation Compatibility

### Prompt / Goal

Move the JWT signing secret out of source code and into an environment variable.

### AI Output / Suggestion

AI initially suggested a PowerShell call using:

`RandomNumberGenerator.Fill(...)`

### What Was Wrong

The available PowerShell/.NET environment did not support that method as expected.

The failed command resulted in an unsafe secret-generation attempt and therefore could not be trusted.

### Fix

The invalid value was not treated as a valid production secret.

The JWT configuration was changed to load `JWT_SECRET` from the environment and enforce a minimum secret length.

A compatible secret-generation approach was used for the local environment.

### Verification

The backend successfully created the JWT encoder/decoder using the environment-provided secret and started normally.

### Commit

https://github.com/kusumaedara517-sketch/TrustVault/commit/b43024b

---

## Entry 11 — 2026-10-20 — Environment Variable Process Scope

### Prompt / Goal

Remove the PostgreSQL password and JWT secret from application source configuration.

### AI Output / Suggestion

AI suggested using Windows `setx` to persist the environment variables.

### What Was Wrong

`setx` affects future processes. The already-running VS Code process did not automatically receive the new values.

The backend therefore initially failed to obtain the database configuration.

### Fix

The variables were loaded into the current terminal session and verified without printing their secret values.

The backend was then restarted from that environment.

### Verification

The application successfully connected to PostgreSQL and Spring Boot started normally.

### Commit

https://github.com/kusumaedara517-sketch/TrustVault/commit/b43024b

---

## Entry 12 — 2026-10-20 — Automated Test Database Safety

### Prompt / Goal

Run an authorization test proving that one user cannot download another user's document.

### AI Output / Suggestion

The first security test used the application's configured database and called `documentRepository.deleteAll()` during setup.

### What Was Wrong

Running destructive automated tests against the development PostgreSQL database could delete real development document records.

This was unsafe test isolation.

### Fix

H2 was added as a test-only dependency.

A separate `src/test/resources/application.properties` configures an in-memory H2 database for tests.

The security test additionally checks that the JDBC URL begins with `jdbc:h2:` before destructive setup runs.

### Verification

`mvnw.cmd test` completed with BUILD SUCCESS using the isolated test configuration.

### Commit

https://github.com/kusumaedara517-sketch/TrustVault/commit/b43024b

---

## Entry 13 — 2026-10-20 — Disabled User With Existing JWT

### Prompt / Goal

Determine whether disabling a user should immediately prevent document access even if that user still has a valid JWT.

### AI Output / Suggestion

AI identified that checking only JWT validity would allow a disabled account to continue using the application until token expiration.

### Fix / Final Design

DocumentService now loads the current user from the database for protected document operations and verifies that the account is enabled.

PermissionService performs the same active-user check for permission management.

A disabled account is denied even when its JWT has not yet expired.

### Security Reason

JWT establishes identity, while current database state determines whether that identity is still permitted to operate.

### Commit

https://github.com/kusumaedara517-sketch/TrustVault/commit/b43024b

---

# Final AI Usage Policy

AI was used as a development assistant for:

- architecture discussion
- Spring Boot implementation guidance
- React implementation guidance
- debugging
- security threat identification
- test design
- documentation

AI output was not automatically trusted.

Suggestions were compiled, manually tested, reviewed against the TrustVault security requirements, and corrected when they were incomplete, incompatible, unsafe, or incorrect.

The most important security principle maintained throughout development was:

> The server decides access every time, and AI suggestions must never automatically widen user access.