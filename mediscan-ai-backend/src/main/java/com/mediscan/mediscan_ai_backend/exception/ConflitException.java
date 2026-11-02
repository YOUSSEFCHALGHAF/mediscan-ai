package com.mediscan.mediscan_ai_backend.exception;

/** Donnée déjà existante (ex. email déjà utilisé) => HTTP 409. */
public class ConflitException extends RuntimeException {
    public ConflitException(String message) {
        super(message);
    }
}