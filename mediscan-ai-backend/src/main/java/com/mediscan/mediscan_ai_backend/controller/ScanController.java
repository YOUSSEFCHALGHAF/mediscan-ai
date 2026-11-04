package com.mediscan.mediscan_ai_backend.controller;


import com.mediscan.mediscan_ai_backend.dto.request.CorrectionScanRequest;
import com.mediscan.mediscan_ai_backend.dto.response.AjoutTraitementResponse;
import com.mediscan.mediscan_ai_backend.dto.response.ScanResponse;
import com.mediscan.mediscan_ai_backend.security.UtilisateurPrincipal;
import com.mediscan.mediscan_ai_backend.service.ScanService;
import com.mediscan.mediscan_ai_backend.service.StockageImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

/**
 * Scans de médicaments (rôle PATIENT, vérifié par SecurityConfig) :
 * POST   /api/patient/scans                    (multipart, champ "image") → 201
 * GET    /api/patient/scans/{id}/image         → la photo
 * PUT    /api/patient/scans/{id}/medicament    → correction manuelle
 * POST   /api/patient/scans/{id}/traitement    → ajout aux traitements en cours
 * DELETE /api/patient/scans/{id}               → suppression définitive (204)
 * (La liste et le détail des scans sont dans PatientController.)
 */
@RestController
@RequestMapping("/api/patient/scans")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ScanResponse> creer(@AuthenticationPrincipal UtilisateurPrincipal moi,
                                              @RequestParam("image") MultipartFile image) {
        ScanResponse scan = scanService.creerScan(moi.getId(), image);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(scan.id()).toUri();
        return ResponseEntity.created(location).body(scan);
    }

    @GetMapping("/{scanId}/image")
    public ResponseEntity<Resource> image(@AuthenticationPrincipal UtilisateurPrincipal moi,
                                          @PathVariable Long scanId) {
        StockageImageService.ImageLue image = scanService.image(moi.getId(), scanId);
        return ResponseEntity.ok()
                .contentType(image.type())
                // "private" : seul le navigateur du patient peut mettre l'image en cache
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(image.ressource());
    }

    @PutMapping("/{scanId}/medicament")
    public ScanResponse corriger(@AuthenticationPrincipal UtilisateurPrincipal moi,
                                 @PathVariable Long scanId,
                                 @Valid @RequestBody CorrectionScanRequest req) {
        return scanService.corrigerMedicament(moi.getId(), scanId, req.medicamentId());
    }

    @PostMapping("/{scanId}/traitement")
    public AjoutTraitementResponse ajouterAuxTraitements(@AuthenticationPrincipal UtilisateurPrincipal moi,
                                                         @PathVariable Long scanId) {
        return scanService.ajouterAuxTraitements(moi.getId(), scanId);
    }

    @DeleteMapping("/{scanId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@AuthenticationPrincipal UtilisateurPrincipal moi, @PathVariable Long scanId) {
        scanService.supprimer(moi.getId(), scanId);
    }
}