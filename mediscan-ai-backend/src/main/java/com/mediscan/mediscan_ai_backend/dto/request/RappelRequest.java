package com.mediscan.mediscan_ai_backend.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

/** Corps de POST et PUT /api/patient/rappels. Heure au format "HH : mm" (ex. "20:00"). */
public record RappelRequest(

        @NotNull(message = "Le médicament est obligatoire")
        Long medicamentId,

        @NotBlank(message = "Le libellé est obligatoire")
        @Size(max = 150)
        String libelle,

        @NotNull(message = "L'heure de prise est obligatoire")
        LocalTime heurePrise,

        @Pattern(regexp = "^(QUOTIDIEN|HEBDOMADAIRE|PONCTUEL)$",
                message = "Fréquence : QUOTIDIEN, HEBDOMADAIRE ou PONCTUEL")
        String frequence,

        @NotNull(message = "La date de début est obligatoire")
        LocalDate dateDebut,

        LocalDate dateFin,

        /** Facultatif : true par défaut à la création. */
        Boolean actif
) {
    /** Règle sur deux champs : la date de fin ne peut pas précéder la date de début. */
    @JsonIgnore
    @AssertTrue(message = "La date de fin doit être postérieure ou égale à la date de début")
    public boolean isPeriodeValide() {
        return dateFin == null || dateDebut == null || !dateFin.isBefore(dateDebut);
    }
}