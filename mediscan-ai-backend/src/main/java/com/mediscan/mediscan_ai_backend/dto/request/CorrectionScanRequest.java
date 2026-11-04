package com.mediscan.mediscan_ai_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Correction manuelle d'un scan : le patient indique le bon médicament de la base. */
public record CorrectionScanRequest(
        @NotNull(message = "Choisissez un médicament")
        @Positive(message = "Identifiant de médicament invalide")
        Long medicamentId) {
}