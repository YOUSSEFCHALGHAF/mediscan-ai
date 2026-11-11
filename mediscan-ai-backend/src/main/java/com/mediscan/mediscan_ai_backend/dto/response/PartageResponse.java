package com.mediscan.mediscan_ai_backend.dto.response;

import com.mediscan.mediscan_ai_backend.entity.PartageDonnees;
import com.mediscan.mediscan_ai_backend.util.PerimetrePartage;

import java.time.LocalDateTime;
import java.util.List;

/** Un consentement de partage (actif ou révoqué), vu par le patient. */
public record PartageResponse(
        Long id,
        MedecinPublicResponse medecin,
        List<String> categories,
        LocalDateTime dateConsentement,
        LocalDateTime dateRevocation,
        boolean actif
) {
    /** Le médecin doit avoir été chargé (EntityGraph ou entité déjà en mémoire). */
    public static PartageResponse from(PartageDonnees p) {
        return new PartageResponse(p.getId(), MedecinPublicResponse.from(p.getMedecin()),
                PerimetrePartage.decouper(p.getPerimetreDonnees()),
                p.getDateConsentement(), p.getDateRevocation(), p.isActif());
    }
}