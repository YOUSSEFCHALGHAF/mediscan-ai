package com.mediscan.mediscan_ai_backend.dto.response;



import com.mediscan.mediscan_ai_backend.entity.Alerte;
import com.mediscan.mediscan_ai_backend.entity.enums.NiveauGravite;
import com.mediscan.mediscan_ai_backend.entity.enums.TypeAlerte;

import java.time.LocalDateTime;

/** Alerte de la file du médecin : les champs d'AlerteResponse + le patient concerné. */
public record AlerteATraiterResponse(
        Long id,
        TypeAlerte type,
        NiveauGravite niveauGravite,
        String message,
        LocalDateTime dateGeneration,
        boolean traitee,
        Long scanId,
        String medicamentNom,
        Long patientId,
        String patientNom
) {
    /** L'alerte doit avoir été chargée avec son patient (EntityGraph). */
    public static AlerteATraiterResponse from(Alerte a) {
        return new AlerteATraiterResponse(a.getId(), a.getType(), a.getNiveauGravite(), a.getMessage(),
                a.getDateGeneration(), a.isTraitee(),
                a.getScan() != null ? a.getScan().getId() : null,
                a.getMedicament() != null ? a.getMedicament().getNomCommercial() : null,
                a.getPatient().getId(),
                a.getPatient().getPrenom() + " " + a.getPatient().getNom());
    }
}