package com.mediscan.mediscan_ai_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mediscan.mediscan_ai_backend.entity.Medecin;
import com.mediscan.mediscan_ai_backend.entity.Patient;
import com.mediscan.mediscan_ai_backend.entity.Utilisateur;
import com.mediscan.mediscan_ai_backend.entity.enums.Role;


import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Informations publiques de l'utilisateur connecté.
 * Ne contient JAMAIS le hash du mot de passe.
 * Les champs null (ex. "specialite" pour un patient) ne sont pas envoyés.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UtilisateurResponse(
        Long id,
        String nom,
        String prenom,
        String email,
        Role role,
        LocalDateTime dateCreation,
        // Champs patient
        LocalDate dateNaissance,
        String sexe,
        String groupeSanguin,
        // Champs médecin
        String numeroRpps,
        String specialite,
        // Commun
        String telephone
) {
    public static UtilisateurResponse from(Utilisateur u) {
        return switch (u) {
            case Patient p -> new UtilisateurResponse(
                    p.getId(), p.getNom(), p.getPrenom(), p.getEmail(), p.getRole(), p.getDateCreation(),
                    p.getDateNaissance(), p.getSexe(), p.getGroupeSanguin(),
                    null, null, p.getTelephone());
            case Medecin m -> new UtilisateurResponse(
                    m.getId(), m.getNom(), m.getPrenom(), m.getEmail(), m.getRole(), m.getDateCreation(),
                    null, null, null,
                    m.getNumeroRpps(), m.getSpecialite(), m.getTelephone());
            default -> new UtilisateurResponse(
                    u.getId(), u.getNom(), u.getPrenom(), u.getEmail(), u.getRole(), u.getDateCreation(),
                    null, null, null, null, null, null);
        };
    }
}