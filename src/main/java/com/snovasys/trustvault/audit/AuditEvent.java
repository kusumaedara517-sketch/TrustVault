package com.snovasys.trustvault.audit;

public enum AuditEvent {

    LOGIN_SUCCESS,
    LOGIN_FAILED,

    DOCUMENT_UPLOAD,
    DOCUMENT_DOWNLOAD,
    DOCUMENT_EDIT,
    DOCUMENT_DELETE,

    ACCESS_GRANTED,
    ACCESS_REVOKED,
    ACCESS_DENIED
}