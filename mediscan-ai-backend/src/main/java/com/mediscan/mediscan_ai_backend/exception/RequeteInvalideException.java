package com.mediscan.mediscan_ai_backend.exception;

/** Requête correcte techniquement, mais refusée par une règle métier → HTTP 400. */
public class RequeteInvalideException extends RuntimeException {
  public RequeteInvalideException(String message) {
    super(message);
  }
}