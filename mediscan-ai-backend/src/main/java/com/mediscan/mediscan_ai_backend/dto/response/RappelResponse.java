package com.mediscan.mediscan_ai_backend.dto.response;


import com.mediscan.mediscan_ai_backend.entity.Rappel;

import java.time.LocalDate;
import java.time.LocalTime;

/** Un rappel de prise. */
public record RappelResponse(
        Long id,
        String libelle,
        LocalTime heurePrise,
        String frequence,
        LocalDate dateDebut,
        LocalDate dateFin,
        boolean actif,
        MedicamentResumeResponse medicament
) {
    public static RappelResponse from(Rappel r) {
        return new RappelResponse(r.getId(), r.getLibelle(), r.getHeurePrise(), r.getFrequence(),
                r.getDateDebut(), r.getDateFin(), r.isActif(),
                MedicamentResumeResponse.from(r.getMedicament()));
    }
}