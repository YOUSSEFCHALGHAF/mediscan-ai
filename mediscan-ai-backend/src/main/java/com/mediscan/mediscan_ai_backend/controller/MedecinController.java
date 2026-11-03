package com.mediscan.mediscan_ai_backend.controller;


import com.mediscan.mediscan_ai_backend.dto.response.FichePatientResponse;
import com.mediscan.mediscan_ai_backend.dto.response.PatientResumeResponse;
import com.mediscan.mediscan_ai_backend.security.UtilisateurPrincipal;
import com.mediscan.mediscan_ai_backend.service.MedecinService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Espace médecin (rôle MEDECIN, vérifié par SecurityConfig). */
@RestController
@RequestMapping("/api/medecin")
@RequiredArgsConstructor
public class MedecinController {

    private final MedecinService medecinService;

    /** Patients qui m'ont donné un consentement actif. */
    @GetMapping("/patients")
    public List<PatientResumeResponse> mesPatients(@AuthenticationPrincipal UtilisateurPrincipal moi) {
        return medecinService.mesPatients(moi.getId());
    }

    /** Fiche d'un patient : 403 si pas de consentement actif. */
    @GetMapping("/patients/{patientId}")
    public FichePatientResponse fichePatient(@AuthenticationPrincipal UtilisateurPrincipal moi,
                                             @PathVariable Long patientId) {
        return medecinService.fichePatient(moi.getId(), patientId);
    }
}