package com.mediscan.mediscan_ai_backend.service;

import com.mediscan.mediscan_ai_backend.dto.response.AjoutTraitementResponse;
import com.mediscan.mediscan_ai_backend.dto.response.AnamneseResponse;
import com.mediscan.mediscan_ai_backend.dto.response.ScanResponse;
import com.mediscan.mediscan_ai_backend.entity.Anamnese;
import com.mediscan.mediscan_ai_backend.entity.Medicament;
import com.mediscan.mediscan_ai_backend.entity.ScanMedicament;
import com.mediscan.mediscan_ai_backend.entity.enums.StatutScan;
import com.mediscan.mediscan_ai_backend.exception.RequeteInvalideException;
import com.mediscan.mediscan_ai_backend.exception.ResourceNotFoundException;
import com.mediscan.mediscan_ai_backend.repository.AnamneseRepository;
import com.mediscan.mediscan_ai_backend.repository.MedicamentRepository;
import com.mediscan.mediscan_ai_backend.repository.PatientRepository;
import com.mediscan.mediscan_ai_backend.repository.ScanMedicamentRepository;
import com.mediscan.mediscan_ai_backend.util.ListeTexte;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Scans de médicaments : création + analyse IA, image, correction manuelle,
 * ajout aux traitements en cours, suppression (RGPD).
 * Toutes les opérations vérifient que le scan appartient au patient connecté.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanService {

    private final ScanMedicamentRepository scanRepository;
    private final PatientRepository patientRepository;
    private final MedicamentRepository medicamentRepository;
    private final AnamneseRepository anamneseRepository;
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
        return stockageImage.lire(trouver(patientId, scanId).getImageUrl());
    }

    /**
     * Correction manuelle : le patient indique le bon médicament.
     * Le scan passe à TERMINE (le patient a validé lui-même l'identification).
     */
    @Transactional
    public ScanResponse corrigerMedicament(Long patientId, Long scanId, Long medicamentId) {
        ScanMedicament scan = trouver(patientId, scanId);
        Medicament medicament = medicamentRepository.findById(medicamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Médicament introuvable"));

        scan.setMedicament(medicament);
        scan.setStatut(StatutScan.TERMINE);
        log.info("Scan {} corrigé manuellement par le patient {} : {}", scanId, patientId, medicament.getNomCommercial());
        // ÉTAPE 16 : les alertes (interactions, allergies...) seront recalculées ici
        return ScanResponse.from(scan);
    }

    /**
     * Ajoute la substance active du médicament scanné aux traitements en cours de l'anamnèse,
     * pour que la détection des interactions (étape 16) en tienne compte.
     */
    @Transactional
    public AjoutTraitementResponse ajouterAuxTraitements(Long patientId, Long scanId) {
        ScanMedicament scan = trouver(patientId, scanId);
        Medicament medicament = scan.getMedicament();
        if (medicament == null) {
            throw new RequeteInvalideException(
                    "Ce scan n'a pas de médicament identifié : indiquez d'abord le bon médicament");
        }
        String substance = medicament.getDci();

        Anamnese anamnese = anamneseRepository.findByPatientId(patientId).orElseGet(() -> {
            Anamnese nouvelle = new Anamnese();
            nouvelle.setPatient(patientRepository.getReferenceById(patientId));
            return nouvelle;
        });

        boolean dejaPresent = ListeTexte.contientTout(anamnese.getTraitementsEnCours(), substance);
        if (!dejaPresent) {
            anamnese.setTraitementsEnCours(ListeTexte.ajouter(anamnese.getTraitementsEnCours(), substance));
            anamnese = anamneseRepository.saveAndFlush(anamnese);
            log.info("Patient {} : « {} » ajouté aux traitements en cours (scan {})", patientId, substance, scanId);
        }
        return new AjoutTraitementResponse(substance, dejaPresent, AnamneseResponse.from(anamnese));
    }

    /**
     * Suppression définitive d'un scan (droit à l'effacement, RGPD) :
     * la ligne en base, puis la photo sur le disque.
     * Les alertes liées sont conservées (scan_id passe à NULL : ON DELETE SET NULL).
     */
    @Transactional
    public void supprimer(Long patientId, Long scanId) {
        ScanMedicament scan = trouver(patientId, scanId);
        String cheminImage = scan.getImageUrl();

        scanRepository.delete(scan);
        scanRepository.flush();

        // La photo n'est effacée qu'APRÈS la validation de la transaction :
        // si la suppression en base échoue, la photo reste cohérente avec la base.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    stockageImage.supprimer(cheminImage);
                }
            });
        } else {
            stockageImage.supprimer(cheminImage);
        }
        log.info("Scan {} supprimé par le patient {}", scanId, patientId);
    }

    /** Le scan du patient, ou 404 (même s'il existe pour un autre patient : on ne le révèle pas). */
    private ScanMedicament trouver(Long patientId, Long scanId) {
        return scanRepository.findByIdAndPatientId(scanId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan introuvable"));
    }
}