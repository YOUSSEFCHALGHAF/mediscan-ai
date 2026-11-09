package com.mediscan.mediscan_ai_backend.controller;


import com.mediscan.mediscan_ai_backend.dto.request.DeviceTokenRequest;
import com.mediscan.mediscan_ai_backend.security.UtilisateurPrincipal;
import com.mediscan.mediscan_ai_backend.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Tokens de notification des navigateurs (patient ET médecin : tout utilisateur connecté).
 * POST   /api/notifications/tokens          → 204
 * DELETE /api/notifications/tokens ? Token=… → 204 (déconnexion)
 */
@RestController
@RequestMapping("/api/notifications/tokens")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enregistrer(@AuthenticationPrincipal UtilisateurPrincipal moi,
                            @Valid @RequestBody DeviceTokenRequest req) {
        notificationService.enregistrerToken(moi.getId(), req.token(), req.navigateur());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@AuthenticationPrincipal UtilisateurPrincipal moi, @RequestParam String token) {
        notificationService.supprimerToken(moi.getId(), token);
    }
}