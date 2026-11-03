package com.mediscan.mediscan_ai_backend.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Corps de PUT /api/patient/anamnese.
 * Tous les champs sont facultatifs. Les listes sont séparées par des virgules,
 * ex. allergies = "amoxicilline, pénicilline".
 */
public record AnamneseRequest(

        @Size(max = 5000, message = "Antécédents : 5000 caractères maximum")
        String antecedents,

        @Size(max = 2000, message = "Allergies : 2000 caractères maximum")
        String allergies,

        @Size(max = 2000, message = "Pathologies chroniques : 2000 caractères maximum")
        String pathologiesChroniques,

        @Size(max = 2000, message = "Traitements en cours : 2000 caractères maximum")
        String traitementsEnCours,

        @Size(max = 2000, message = "Habitudes de vie : 2000 caractères maximum")
        String habitudesVie
) {}