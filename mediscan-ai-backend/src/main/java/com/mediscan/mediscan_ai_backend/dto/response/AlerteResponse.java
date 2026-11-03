package com.mediscan.mediscan_ai_backend.dto.response;

import com.mediscan.mediscan_ai_backend.entity.Alerte;
import com.mediscan.mediscan_ai_backend.entity.enums.NiveauGravite;
import com.mediscan.mediscan_ai_backend.entity.enums.TypeAlerte;

import java.time.LocalDateTime;

/** Une alerte (interaction, contre-indication, allergie, expiration). */
public record AlerteResponse(
        Long id,
        TypeAlerte type,
        NiveauGravite niveauGravite,
        String message,
        LocalDateTime dateGeneration,
        boolean traitee,
        Long scanId,
        String medicamentNom
) {
    public static AlerteResponse from(Alerte a) {
        return new AlerteResponse(a.getId(), a.getType(), a.getNiveauGravite(), a.getMessage(),
                a.getDateGeneration(), a.isTraitee(),
                a.getScan() != null ? a.getScan().getId() : null,
                a.getMedicament() != null ? a.getMedicament().getNomCommercial() : null);
    }
}