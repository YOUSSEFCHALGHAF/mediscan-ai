package com.mediscan.mediscan_ai_backend.dto.request;

import jakarta.validation.constraints.*;

/** Corps de POST /api/auth/register/medecin */
public record RegisterMedecinRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100)
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(max = 100)
        String prenom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        @Size(max = 150)
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
                message = "Le mot de passe doit contenir 8 à 72 caractères, dont au moins une lettre et un chiffre")
        String motDePasse,

        @NotBlank(message = "Le numéro RPPS est obligatoire")
        @Pattern(regexp = "^\\d{11}$", message = "Le numéro RPPS doit contenir exactement 11 chiffres")
        String numeroRpps,

        @NotBlank(message = "La spécialité est obligatoire")
        @Size(max = 100)
        String specialite,

        @Pattern(regexp = "^\\+?[0-9 ]{8,20}$", message = "Numéro de téléphone invalide")
        String telephone
) {}