package com.mediscan.mediscan_ai_backend.controller;

import com.mediscan.mediscan_ai_backend.dto.request.RappelRequest;
import com.mediscan.mediscan_ai_backend.dto.response.RappelResponse;
import com.mediscan.mediscan_ai_backend.security.UtilisateurPrincipal;
import com.mediscan.mediscan_ai_backend.service.RappelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** CRUD des rappels du patient connecté. */
@RestController
@RequestMapping("/api/patient/rappels")
@RequiredArgsConstructor
public class RappelController {

    private final RappelService rappelService;

    @GetMapping
    public List<RappelResponse> lister(@AuthenticationPrincipal UtilisateurPrincipal moi) {
        return rappelService.lister(moi.getId());
    }

    @GetMapping("/{id}")
    public RappelResponse lire(@AuthenticationPrincipal UtilisateurPrincipal moi, @PathVariable Long id) {
        return rappelService.lire(moi.getId(), id);
    }

    /** 201 Created + en-tête Location vers le nouveau rappel. */
    @PostMapping
    public ResponseEntity<RappelResponse> creer(@AuthenticationPrincipal UtilisateurPrincipal moi,
                                                @Valid @RequestBody RappelRequest req) {
        RappelResponse cree = rappelService.creer(moi.getId(), req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(cree.id()).toUri();
        return ResponseEntity.created(location).body(cree);
    }

    @PutMapping("/{id}")
    public RappelResponse modifier(@AuthenticationPrincipal UtilisateurPrincipal moi,
                                   @PathVariable Long id,
                                   @Valid @RequestBody RappelRequest req) {
        return rappelService.modifier(moi.getId(), id, req);
    }

    /** 204 No Content. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@AuthenticationPrincipal UtilisateurPrincipal moi,
                                          @PathVariable Long id) {
        rappelService.supprimer(moi.getId(), id);
        return ResponseEntity.noContent().build();
    }
}