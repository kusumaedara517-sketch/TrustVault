# TrustVault

TrustVault is a secure document-sharing web application developed for the Snovasys Fresher Hiring Challenge 2026.

Users can upload private documents, share them with specific users using READ or EDIT permissions, revoke access immediately, search accessible documents, and maintain an audit trail of security-sensitive actions.

## Core Security Principle

> Knowing a document ID is never enough to access a document.

Authentication establishes identity, but the backend checks the current authorization for every protected document operation.

---

## Features

### Authentication

- User registration and login
- BCrypt password hashing
- JWT authentication
- Stateless Spring Security
- Environment-based secrets
- Disabled-user access checks

### Document Management

- Upload documents
- Private document storage
- Search documents
- Download accessible documents
- Edit document category
- Delete owned documents
- Upload validation
- Random generated storage keys

### Access Control

- Per-document READ permission
- Per-document EDIT permission
- Owner-only permission management
- Immediate access revocation
- Backend authorization on sensitive operations
- Deny-by-default design

### Search

Users can search accessible documents by:

- filename
- owner
- category
- date

Search results are filtered according to current ownership and permissions.

### Audit Trail

TrustVault records:

- successful logins
- failed logins
- document uploads
- downloads
- edits
- deletions
- access grants
- access revocations
- refused access attempts

Audit records use a hash-chain design to provide application-level tamper evidence.

### Administration

Administrators can inspect:

- audit activity
- active document permissions

ADMIN status does not automatically grant access to private document contents.

---

## Roles

### VIEWER

Normal registered users receive the VIEWER role.

A viewer can upload documents and becomes the owner of documents they upload.

### OWNER

OWNER exists in the role model, but actual document ownership is determined by the document's owner relationship in the database.

### ADMIN

Administrators can inspect audit and permission information.

Admin status does not bypass normal document-content authorization.

---

## Permission Model

TrustVault supports:

### READ

Allows the named user to download the shared document.

### EDIT

Allows the named user to download the document and edit supported document metadata.

Only the document owner can grant or revoke permissions.

Document permissions are stored in PostgreSQL and checked when protected requests are made.

Permissions are not stored as document grants inside the JWT. Therefore, revoking access takes effect immediately even if the shared user still has an unexpired JWT.

---

## Technology Stack

### Frontend

- React
- Vite
- JavaScript
- HTML
- CSS

### Backend

- Java 21 target
- Spring Boot 4
- Spring Web MVC
- Spring Security
- OAuth2 Resource Server JWT
- Spring Data JPA
- BCrypt

### Database

- PostgreSQL

### Testing

- JUnit
- Spring Boot Test
- H2 in-memory test database

### Build

- Maven

---

## Project Structure

```text
trustvault/
├── src/
│   ├── main/
│   │   ├── java/com/snovasys/trustvault/
│   │   │   ├── admin/
│   │   │   ├── audit/
│   │   │   ├── document/
│   │   │   ├── security/
│   │   │   ├── user/
│   │   │   └── TrustvaultApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/
│       └── resources/
├── trustvault-frontend/
├── THREATS.md
├── PROMPTS.md
├── README.md
├── pom.xml
└── .gitignore