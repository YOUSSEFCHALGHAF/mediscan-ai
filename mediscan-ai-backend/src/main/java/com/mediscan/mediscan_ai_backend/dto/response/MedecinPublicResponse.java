package com.mediscan.mediscan_ai_backend.dto.response;


import com.mediscan.mediscan_ai_backend.entity.Medecin;

/** Médecin tel que le patient le voit : ni e-mail, ni téléphone, ni numéro RPPS. */
public record MedecinPublicResponse(Long id, String nom, String prenom, String specialite) {

    public static MedecinPublicResponse from(Medecin m) {
        return new MedecinPublicResponse(m.getId(), m.getNom(), m.getPrenom(), m.getSpecialite());
    }
}