package com.mediscan.mediscan_ai_backend.exception;

/** Le médecin tente d'accéder à un patient sans consentement actif → HTTP 403. */
public class ConsentementRequisException extends RuntimeException {
    public ConsentementRequisException(String message) {
        super(message);
    }
}