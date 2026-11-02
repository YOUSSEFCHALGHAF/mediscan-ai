package com.mediscan.mediscan_ai_backend.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Corps de POST /api/auth/register/patient */
public record RegisterPatientRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères")
        String prenom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        @Size(max = 150, message = "L'email ne doit pas dépasser 150 caractères")
        String email,

        /* 8 à 72 caractères (limite de BCrypt), au moins une lettre et un chiffre */
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
                message = "Le mot de passe doit contenir 8 à 72 caractères, dont au moins une lettre et un chiffre")
        String motDePasse,

        @Past(message = "La date de naissance doit être dans le passé")
        LocalDate dateNaissance,

        @Pattern(regexp = "^(HOMME|FEMME|AUTRE)$", message = "Sexe : HOMME, FEMME ou AUTRE")
        String sexe,

        @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Groupe sanguin invalide (ex. A+, O-)")
        String groupeSanguin,

        @Pattern(regexp = "^\\+?[0-9 ]{8,20}$", message = "Numéro de téléphone invalide")
        String telephone
) {}