package com.mediscan.mediscan_ai_backend.controller;


import com.mediscan.mediscan_ai_backend.dto.response.AlerteATraiterResponse;
import com.mediscan.mediscan_ai_backend.dto.response.TableauDeBordMedecinResponse;
import com.mediscan.mediscan_ai_backend.security.UtilisateurPrincipal;
import com.mediscan.mediscan_ai_backend.service.TableauDeBordMedecinService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Tableau de bord du médecin (rôle MEDECIN, vérifié par SecurityConfig) :
 * GET /api/medecin/tableau-de-bord → indicateurs, graphiques, file des 10 alertes prioritaires
 * GET /api/medecin/alertes         → toutes les alertes non traitées de ses patients
 */
@RestController
@RequestMapping("/api/medecin")
@RequiredArgsConstructor
public class TableauDeBordMedecinController {

    private final TableauDeBordMedecinService tableauDeBordService;

    @GetMapping("/tableau-de-bord")
    public TableauDeBordMedecinResponse tableauDeBord(@AuthenticationPrincipal UtilisateurPrincipal moi) {
        return tableauDeBordService.tableauDeBord(moi.getId(), LocalDate.now());
    }

    @GetMapping("/alertes")
    public List<AlerteATraiterResponse> alertesATraiter(@AuthenticationPrincipal UtilisateurPrincipal moi) {
        return tableauDeBordService.alertesATraiter(moi.getId());
    }
}