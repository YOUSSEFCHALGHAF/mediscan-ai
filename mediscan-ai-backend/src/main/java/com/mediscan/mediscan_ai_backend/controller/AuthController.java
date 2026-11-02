package com.mediscan.mediscan_ai_backend.controller;


import com.mediscan.mediscan_ai_backend.dto.request.LoginRequest;
import com.mediscan.mediscan_ai_backend.dto.request.RegisterMedecinRequest;
import com.mediscan.mediscan_ai_backend.dto.request.RegisterPatientRequest;
import com.mediscan.mediscan_ai_backend.dto.response.AuthResponse;
import com.mediscan.mediscan_ai_backend.dto.response.UtilisateurResponse;
import com.mediscan.mediscan_ai_backend.security.UtilisateurPrincipal;
import com.mediscan.mediscan_ai_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentification :
 * POST /api/auth/register/patient  (public)  -> 201
 * POST /api/auth/register/medecin  (public)  -> 201
 * POST /api/auth/login             (public)  -> 200
 * GET  /api/auth/me                (connecté) -> 200
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/patient")
    public ResponseEntity<AuthResponse> inscrirePatient(@Valid @RequestBody RegisterPatientRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.inscrirePatient(req));
    }

    @PostMapping("/register/medecin")
    public ResponseEntity<AuthResponse> inscrireMedecin(@Valid @RequestBody RegisterMedecinRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.inscrireMedecin(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> connecter(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.connecter(req));
    }

    @GetMapping("/me")
    public ResponseEntity<UtilisateurResponse> moi(@AuthenticationPrincipal UtilisateurPrincipal principal) {
        return ResponseEntity.ok(authService.profilConnecte(principal.getId()));
    }
}