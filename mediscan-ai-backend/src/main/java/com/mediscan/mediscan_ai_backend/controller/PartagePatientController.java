package com.mediscan.mediscan_ai_backend.controller;

import com.mediscan.mediscan_ai_backend.dto.request.PartageRequest;
import com.mediscan.mediscan_ai_backend.dto.request.PerimetreRequest;
import com.mediscan.mediscan_ai_backend.dto.response.MedecinPublicResponse;
import com.mediscan.mediscan_ai_backend.dto.response.PartageResponse;
import com.mediscan.mediscan_ai_backend.security.UtilisateurPrincipal;
import com.mediscan.mediscan_ai_backend.service.PartagePatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Consentements du patient (rôle PATIENT, vérifié par SecurityConfig) :
 * GET    /api/patient/partages            → mes partages (actifs et révoqués)
 * POST   /api/patient/partages            → donner accès (201)
 * PUT    /api/patient/partages/{id}       → changer les catégories (nouveau consentement)
 * DELETE /api/patient/partages/{id}       → révoquer (204)
 * GET    /api/patient/medecins ?q=…        → rechercher un médecin
 */
@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PartagePatientController {

    private final PartagePatientService partageService;

    @GetMapping("/partages")
    public List<PartageResponse> lister(@AuthenticationPrincipal UtilisateurPrincipal moi) {
        return partageService.lister(moi.getId());
    }

    @PostMapping("/partages")
    public ResponseEntity<PartageResponse> creer(@AuthenticationPrincipal UtilisateurPrincipal moi,
                                                 @Valid @RequestBody PartageRequest req) {
        PartageResponse partage = partageService.creer(moi.getId(), req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(partage.id()).toUri();
        return ResponseEntity.created(location).body(partage);
    }

    @PutMapping("/partages/{partageId}")
    public PartageResponse modifier(@AuthenticationPrincipal UtilisateurPrincipal moi,
                                    @PathVariable Long partageId,
                                    @Valid @RequestBody PerimetreRequest req) {
        return partageService.modifierPerimetre(moi.getId(), partageId, req);
    }

    @DeleteMapping("/partages/{partageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoquer(@AuthenticationPrincipal UtilisateurPrincipal moi, @PathVariable Long partageId) {
        partageService.revoquer(moi.getId(), partageId);
    }

    @GetMapping("/medecins")
    public List<MedecinPublicResponse> rechercherMedecins(@RequestParam String q) {
        return partageService.rechercherMedecins(q);
    }
}