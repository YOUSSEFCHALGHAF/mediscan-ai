package com.mediscan.mediscan_ai_backend.controller;

import com.mediscan.mediscan_ai_backend.security.UtilisateurPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * TEMPORAIRE (étape 5) : vérifie les règles d'accès par rôle.
 * Sera supprimé à l'étape 6, quand les vrais contrôleurs existeront.
 */
@RestController
public class AccesTestController {

    @GetMapping("/api/patient/ping")
    public Map<String, Object> pingPatient(@AuthenticationPrincipal UtilisateurPrincipal p) {
        return Map.of("message", "Accès PATIENT autorisé", "utilisateur", p.getNomComplet());
    }

    @GetMapping("/api/medecin/ping")
    public Map<String, Object> pingMedecin(@AuthenticationPrincipal UtilisateurPrincipal p) {
        return Map.of("message", "Accès MEDECIN autorisé", "utilisateur", p.getNomComplet());
    }
}