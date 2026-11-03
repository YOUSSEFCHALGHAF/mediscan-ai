package com.mediscan.mediscan_ai_backend.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Corps de PUT /api/patient/profil (l'email et le mot de passe ne se modifient pas ici). */
public record ProfilPatientRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100)
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(max = 100)
        String prenom,

        @Past(message = "La date de naissance doit être dans le passé")
        LocalDate dateNaissance,

        @Pattern(regexp = "^(HOMME|FEMME|AUTRE)$", message = "Sexe : HOMME, FEMME ou AUTRE")
        String sexe,

        @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Groupe sanguin invalide (ex. A+, O-)")
        String groupeSanguin,

        @Pattern(regexp = "^\\+?[0-9 ]{8,20}$", message = "Numéro de téléphone invalide")
        String telephone
) {}