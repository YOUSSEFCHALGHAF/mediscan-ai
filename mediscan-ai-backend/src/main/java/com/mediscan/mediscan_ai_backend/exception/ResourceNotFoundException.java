package com.mediscan.mediscan_ai_backend.exception;

/** Ressource introuvable (ex. patient inexistant) → HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}