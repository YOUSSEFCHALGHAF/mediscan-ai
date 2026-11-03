package com.mediscan.mediscan_ai_backend.controller;

import com.mediscan.mediscan_ai_backend.dto.request.AnamneseRequest;
import com.mediscan.mediscan_ai_backend.dto.request.ProfilPatientRequest;
import com.mediscan.mediscan_ai_backend.dto.response.AnamneseResponse;
import com.mediscan.mediscan_ai_backend.dto.response.ScanResponse;
import com.mediscan.mediscan_ai_backend.dto.response.UtilisateurResponse;
import com.mediscan.mediscan_ai_backend.security.UtilisateurPrincipal;
import com.mediscan.mediscan_ai_backend.service.AnamneseService;
import com.mediscan.mediscan_ai_backend.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Espace patient (rôle PATIENT, vérifié par SecurityConfig).
 * L'id du patient vient TOUJOURS du token, jamais de l'URL.
 */
@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final AnamneseService anamneseService;

    @GetMapping("/profil")
    public UtilisateurResponse profil(@AuthenticationPrincipal UtilisateurPrincipal moi) {
        return patientService.profil(moi.getId());
    }

    @PutMapping("/profil")
    public UtilisateurResponse modifierProfil(@AuthenticationPrincipal UtilisateurPrincipal moi,
                                              @Valid @RequestBody ProfilPatientRequest req) {
        return patientService.modifierProfil(moi.getId(), req);
    }

    @GetMapping("/anamnese")
    public AnamneseResponse anamnese(@AuthenticationPrincipal UtilisateurPrincipal moi) {
        return anamneseService.lire(moi.getId());
    }

    @PutMapping("/anamnese")
    public AnamneseResponse enregistrerAnamnese(@AuthenticationPrincipal UtilisateurPrincipal moi,
                                                @Valid @RequestBody AnamneseRequest req) {
        return anamneseService.enregistrer(moi.getId(), req);
    }

    @GetMapping("/scans")
    public List<ScanResponse> scans(@AuthenticationPrincipal UtilisateurPrincipal moi) {
        return patientService.historiqueScans(moi.getId());
    }

    @GetMapping("/scans/{scanId}")
    public ScanResponse scan(@AuthenticationPrincipal UtilisateurPrincipal moi,
                             @PathVariable Long scanId) {
        return patientService.scan(moi.getId(), scanId);
    }
}