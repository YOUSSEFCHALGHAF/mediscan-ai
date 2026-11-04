package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.dto.response.ScanResponse;
import com.mediscan.mediscan_ai_backend.entity.ScanMedicament;
import com.mediscan.mediscan_ai_backend.entity.enums.StatutScan;
import com.mediscan.mediscan_ai_backend.exception.ResourceNotFoundException;
import com.mediscan.mediscan_ai_backend.repository.PatientRepository;
import com.mediscan.mediscan_ai_backend.repository.ScanMedicamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Création des scans de médicaments, analyse IA et accès aux images.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanService {

    private final ScanMedicamentRepository scanRepository;
    private final PatientRepository patientRepository;
    private final StockageImageService stockageImage;
    private final AnalyseScanService analyseScanService;

    /**
     * Enregistre l'image, crée le scan (EN_COURS) puis lance l'analyse IA.
     * Le statut final (TERMINE, A_VERIFIER ou ECHEC) est enregistré à la fin de la transaction.
     */
    @Transactional
    public ScanResponse creerScan(Long patientId, MultipartFile image) {
        StockageImageService.ImageStockee stockee = stockageImage.enregistrer(image);
        try {
            ScanMedicament scan = new ScanMedicament();
            scan.setPatient(patientRepository.getReferenceById(patientId));
            scan.setImageUrl(stockee.cheminRelatif());
            scan.setStatut(StatutScan.EN_COURS);

            ScanMedicament enregistre = scanRepository.saveAndFlush(scan);
            log.info("Scan {} créé pour le patient {} ({}x{} px)",
                    enregistre.getId(), patientId, stockee.largeur(), stockee.hauteur());

            // Analyse IA : prétraitement, OCR, extraction, rapprochement avec la base
            analyseScanService.analyser(enregistre);

            return ScanResponse.from(enregistre);
        } catch (RuntimeException e) {
            // Si la base refuse l'enregistrement, on ne laisse pas d'image orpheline sur le disque
            stockageImage.supprimer(stockee.cheminRelatif());
            throw e;
        }
    }

    /** Image d'un scan, uniquement pour son propriétaire (sinon 404). */
    @Transactional(readOnly = true)
    public StockageImageService.ImageLue image(Long patientId, Long scanId) {
        ScanMedicament scan = scanRepository.findByIdAndPatientId(scanId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan introuvable"));
        return stockageImage.lire(scan.getImageUrl());
    }
}