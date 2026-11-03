package com.mediscan.mediscan_ai_backend.dto.response;


import com.mediscan.mediscan_ai_backend.entity.Anamnese;

import java.time.LocalDateTime;

/** Anamnèse du patient. "complete" = au moins un champ renseigné. */
public record AnamneseResponse(
        Long id,
        String antecedents,
        String allergies,
        String pathologiesChroniques,
        String traitementsEnCours,
        String habitudesVie,
        LocalDateTime dateMaj,
        boolean complete
) {
    public static AnamneseResponse from(Anamnese a) {
        boolean complete = a.getAntecedents() != null || a.getAllergies() != null
                || a.getPathologiesChroniques() != null || a.getTraitementsEnCours() != null
                || a.getHabitudesVie() != null;
        return new AnamneseResponse(a.getId(), a.getAntecedents(), a.getAllergies(),
                a.getPathologiesChroniques(), a.getTraitementsEnCours(), a.getHabitudesVie(),
                a.getDateMaj(), complete);
    }

    /** Anamnèse jamais remplie : Angular affiche un formulaire vide. */
    public static AnamneseResponse vide() {
        return new AnamneseResponse(null, null, null, null, null, null, null, false);
    }
}