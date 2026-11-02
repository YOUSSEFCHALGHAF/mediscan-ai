package com.mediscan.mediscan_ai_backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Format unique des erreurs renvoyées par l'API.
 * Exemple :
 * { "status": 409, "erreur": "Conflict", "message": "Un compte existe déjà avec cet email",
 *   "chemin": "/api/auth/register/patient", "timestamp": "2026-11-02T10:00:00Z" }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String erreur,
        String message,
        String chemin,
        String timestamp,
        Map<String, String> details   // erreurs de validation champ par champ (sinon absent)
) {
    public static ApiError of(int status, String erreur, String message, String chemin) {
        return new ApiError(status, erreur, message, chemin, Instant.now().toString(), null);
    }

    public static ApiError of(int status, String erreur, String message, String chemin,
                              Map<String, String> details) {
        return new ApiError(status, erreur, message, chemin, Instant.now().toString(), details);
    }
}