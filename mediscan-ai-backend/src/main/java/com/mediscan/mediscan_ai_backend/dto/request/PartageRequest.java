package com.mediscan.mediscan_ai_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Le patient donne accès à un médecin, pour les catégories choisies. */
public record PartageRequest(
        @NotNull(message = "Choisissez un médecin")
        Long medecinId,

        @NotEmpty(message = "Choisissez au moins une catégorie de données à partager")
        List<String> categories) {
}