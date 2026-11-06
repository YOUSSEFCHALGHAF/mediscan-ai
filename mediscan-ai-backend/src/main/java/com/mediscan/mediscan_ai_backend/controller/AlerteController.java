package com.mediscan.mediscan_ai_backend.controller;


import com.mediscan.mediscan_ai_backend.dto.response.AlerteResponse;
import com.mediscan.mediscan_ai_backend.security.UtilisateurPrincipal;
import com.mediscan.mediscan_ai_backend.service.AlerteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Alertes médicamenteuses. Les rôles sont vérifiés par SecurityConfig selon le préfixe :
 * GET /api/patient/alertes                                          (PATIENT) toutes mes alertes
 * GET /api/patient/scans/{scanId}/alertes                           (PATIENT) alertes d'un de mes scans
 * PUT /api/medecin/patients/{patientId}/alertes/{alerteId}/traitee  (MEDECIN) marquer comme traitée.
 */
@RestController
@RequiredArgsConstructor
public class AlerteController {

    private final AlerteService alerteService;

    @GetMapping("/api/patient/alertes")
    public List<AlerteResponse> mesAlertes(@AuthenticationPrincipal UtilisateurPrincipal moi) {
        return alerteService.alertesPatient(moi.getId());
    }

    @GetMapping("/api/patient/scans/{scanId}/alertes")
    public List<AlerteResponse> alertesScan(@AuthenticationPrincipal UtilisateurPrincipal moi,
                                            @PathVariable Long scanId) {
        return alerteService.alertesScan(moi.getId(), scanId);
    }

    @PutMapping("/api/medecin/patients/{patientId}/alertes/{alerteId}/traitee")
    public AlerteResponse marquerTraitee(@AuthenticationPrincipal UtilisateurPrincipal moi,
                                         @PathVariable Long patientId,
                                         @PathVariable Long alerteId) {
        return alerteService.marquerTraitee(moi.getId(), patientId, alerteId);
    }
}