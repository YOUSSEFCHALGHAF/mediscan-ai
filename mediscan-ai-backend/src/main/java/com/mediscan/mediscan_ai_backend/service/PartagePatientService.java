package com.mediscan.mediscan_ai_backend.service;

import com.mediscan.mediscan_ai_backend.dto.request.PartageRequest;
import com.mediscan.mediscan_ai_backend.dto.request.PerimetreRequest;
import com.mediscan.mediscan_ai_backend.dto.response.MedecinPublicResponse;
import com.mediscan.mediscan_ai_backend.dto.response.PartageResponse;
import com.mediscan.mediscan_ai_backend.entity.Medecin;
import com.mediscan.mediscan_ai_backend.entity.PartageDonnees;
import com.mediscan.mediscan_ai_backend.event.PartageAccordeEvenement;
import com.mediscan.mediscan_ai_backend.exception.ConflitException;
import com.mediscan.mediscan_ai_backend.exception.RequeteInvalideException;
import com.mediscan.mediscan_ai_backend.exception.ResourceNotFoundException;
import com.mediscan.mediscan_ai_backend.repository.MedecinRepository;
import com.mediscan.mediscan_ai_backend.repository.PartageDonneesRepository;
import com.mediscan.mediscan_ai_backend.repository.PatientRepository;
import com.mediscan.mediscan_ai_backend.util.PerimetrePartage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consentements du patient (côté patient).
 * Règles  – un seul partage ACTIF par médecin  – une révocation ne supprime jamais la ligne (traçabilité, chk_partages_coherence)  – modifier le périmètre = révoquer l'ancien consentement + en créer un nouveau (historique complet).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartagePatientService {

    private static final int LONGUEUR_MIN_RECHERCHE = 2;
    private static final int MAX_RESULTATS = 20;

    private final PartageDonneesRepository partageRepository;
    private final MedecinRepository medecinRepository;
    private final PatientRepository patientRepository;
    private final ApplicationEventPublisher evenements;

    /** Tous les partages du patient, du plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public List<PartageResponse> lister(Long patientId) {
        return partageRepository.findByPatientIdOrderByDateConsentementDesc(patientId).stream()
                .map(PartageResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MedecinPublicResponse> rechercherMedecins(String terme) {
        // Les jokers SQL tapés par l'utilisateur sont retirés : "%" seul ne doit pas lister tous les médecins
        String q = terme == null ? "" : terme.replace("%", "").replace("_", "").trim();
        if (q.length() < LONGUEUR_MIN_RECHERCHE) {
            throw new RequeteInvalideException("Tapez au moins 2 caractères pour rechercher un médecin");
        }
        return medecinRepository.rechercher(q, PageRequest.of(0, MAX_RESULTATS)).stream()
                .map(MedecinPublicResponse::from).toList();
    }

    @Transactional
    public PartageResponse creer(Long patientId, PartageRequest req) {
        Medecin medecin = medecinRepository.findById(req.medecinId())
                .filter(Medecin::isActif)
                .orElseThrow(() -> new ResourceNotFoundException("Médecin introuvable"));
        if (partageRepository.existsByPatientIdAndMedecinIdAndActifTrue(patientId, medecin.getId())) {
            throw new ConflitException(
                    "Ce médecin a déjà accès à vos données : modifiez les catégories du partage existant");
        }

        PartageDonnees partage = enregistrer(patientId, medecin, perimetre(req.categories()));
        log.info("Patient {} : accès accordé au médecin {} ({})", patientId, medecin.getId(), partage.getPerimetreDonnees());

        // Notification au médecin, APRÈS validation de la transaction (étape 17)
        evenements.publishEvent(new PartageAccordeEvenement(medecin.getId(), patientId));
        return PartageResponse.from(partage);
    }

    /**
     * Change les catégories partagées. L'ancien consentement est révoqué et un nouveau est créé,
     * pour qu'on puisse toujours savoir qui avait accès à quoi, et quand.
     */
    @Transactional
    public PartageResponse modifierPerimetre(Long patientId, Long partageId, PerimetreRequest req) {
        PartageDonnees actuel = trouver(patientId, partageId);
        if (!actuel.isActif()) {
            throw new ConflitException("Cet accès a été retiré : donnez un nouvel accès à ce médecin");
        }
        String nouveauPerimetre = perimetre(req.categories());
        String perimetreActuel = String.join(",", PerimetrePartage.decouper(actuel.getPerimetreDonnees()));
        if (nouveauPerimetre.equals(perimetreActuel)) {
            return PartageResponse.from(actuel); // rien ne change : pas de nouvelle ligne dans l'historique
        }

        actuel.revoquer();
        partageRepository.saveAndFlush(actuel);
        PartageDonnees nouveau = enregistrer(patientId, actuel.getMedecin(), nouveauPerimetre);
        log.info("Patient {} : périmètre du médecin {} modifié ({} -> {})",
                patientId, actuel.getMedecin().getId(), perimetreActuel, nouveauPerimetre);
        return PartageResponse.from(nouveau);
    }

    /** Révocation immédiate. Sans effet si l'accès est déjà retiré (opération idempotente). */
    @Transactional
    public void revoquer(Long patientId, Long partageId) {
        PartageDonnees partage = trouver(patientId, partageId);
        if (partage.isActif()) {
            partage.revoquer();
            log.info("Patient {} : accès du médecin {} révoqué", patientId, partage.getMedecin().getId());
        }
    }

    // ------------------------------------------------------------------

    private PartageDonnees enregistrer(Long patientId, Medecin medecin, String perimetre) {
        PartageDonnees partage = new PartageDonnees();
        partage.setPatient(patientRepository.getReferenceById(patientId));
        partage.setMedecin(medecin);
        partage.setPerimetreDonnees(perimetre);
        partage.setActif(true);
        return partageRepository.save(partage);
    }

    private static String perimetre(List<String> categories) {
        try {
            return PerimetrePartage.normaliser(categories);
        } catch (IllegalArgumentException e) {
            throw new RequeteInvalideException(e.getMessage());
        }
    }

    /** Le partage du patient, ou 404 (même s'il existe pour un autre patient : on ne le révèle pas). */
    private PartageDonnees trouver(Long patientId, Long partageId) {
        return partageRepository.findByIdAndPatientId(partageId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Partage introuvable"));
    }
}