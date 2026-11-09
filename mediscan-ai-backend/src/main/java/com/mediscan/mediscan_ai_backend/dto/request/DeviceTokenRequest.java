package com.mediscan.mediscan_ai_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Enregistrement du token FCM d'un navigateur. */
public record DeviceTokenRequest(
        @NotBlank(message = "Le token est obligatoire")
        @Size(max = 512)
        String token,

        @Size(max = 100)
        String navigateur) {
}