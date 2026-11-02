package com.mediscan.mediscan_ai_backend.dto.response;


/** Réponse de connexion / inscription : le token + l'utilisateur. */
public record AuthResponse(
        String token,
        String type,          // toujours "Bearer"
        long expiresIn,       // durée de validité en millisecondes
        UtilisateurResponse utilisateur
) {
    public static AuthResponse of(String token, long expiresIn, UtilisateurResponse utilisateur) {
        return new AuthResponse(token, "Bearer", expiresIn, utilisateur);
    }
}