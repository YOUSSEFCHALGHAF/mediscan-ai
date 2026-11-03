package com.mediscan.mediscan_ai_backend.dto.response;

import com.mediscan.mediscan_ai_backend.entity.PartageDonnees;
import com.mediscan.mediscan_ai_backend.entity.Patient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/** Ligne de la liste "Mes patients" du médecin. */
public record PatientResumeResponse(
        Long id,
        String nom,
        String prenom,
        Integer age,
        String sexe,
        String groupeSanguin,
        long alertesNonTraitees,
        String perimetreDonnees,
        LocalDateTime dateConsentement
) {
    public static PatientResumeResponse from(PartageDonnees partage, long alertesNonTraitees) {
        Patient p = partage.getPatient();
        return new PatientResumeResponse(p.getId(), p.getNom(), p.getPrenom(),
                calculerAge(p.getDateNaissance()), p.getSexe(), p.getGroupeSanguin(),
                alertesNonTraitees, partage.getPerimetreDonnees(), partage.getDateConsentement());
    }

    /** Âge en années (null si la date de naissance est inconnue). */
    public static Integer calculerAge(LocalDate dateNaissance) {
        return dateNaissance == null ? null : Period.between(dateNaissance, LocalDate.now()).getYears();
    }
}