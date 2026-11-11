package com.mediscan.mediscan_ai_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Nouvelles catégories d'un partage existant. */
public record PerimetreRequest(
        @NotEmpty(message = "Choisissez au moins une catégorie de données à partager")
        List<String> categories) {
}