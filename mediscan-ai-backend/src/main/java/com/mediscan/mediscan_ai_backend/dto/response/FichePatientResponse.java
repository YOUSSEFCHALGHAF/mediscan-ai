package com.mediscan.mediscan_ai_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fiche synthétique d'un patient pour le médecin.
 * Seules les catégories autorisées par le patient (perimetreDonnees) sont remplies :
 * les autres valent null et ne sont PAS envoyées (NON_NULL).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FichePatientResponse(
        Long id,
        String nom,
        String prenom,
        String email,
        String telephone,
        LocalDate dateNaissance,
        Integer age,
        String sexe,
        String groupeSanguin,
        String perimetreDonnees,
        LocalDateTime dateConsentement,
        AnamneseResponse anamnese,
        List<ScanResponse> scans,
        List<AlerteResponse> alertes,
        List<RappelResponse> rappels
) {}