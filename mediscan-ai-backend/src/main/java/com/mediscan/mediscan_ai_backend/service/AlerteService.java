package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.dto.response.AlerteResponse;
import com.mediscan.mediscan_ai_backend.entity.Alerte;
import com.mediscan.mediscan_ai_backend.entity.Medicament;
import com.mediscan.mediscan_ai_backend.entity.PartageDonnees;
import com.mediscan.mediscan_ai_backend.entity.ScanMedicament;
import com.mediscan.mediscan_ai_backend.event.AlertesGravesEvenement;
import com.mediscan.mediscan_ai_backend.exception.ConsentementRequisException;
import com.mediscan.mediscan_ai_backend.exception.ResourceNotFoundException;
import com.mediscan.mediscan_ai_backend.ia.InteractionRulesEngine;
import com.mediscan.mediscan_ai_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.mediscan.mediscan_ai_backend.entity.enums.NiveauGravite;
import org.springframework.context.ApplicationEventPublisher;
/**
 * Alertes de sécurité médicamenteuse  – génération automatique (moteur de règles) après chaque analyse ou correction de scan  – consultation par le patient  – traitement par le médecin (avec consentement ACTIF couvrant les alertes).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlerteService {

    private static final int LONGUEUR_MAX_MESSAGE = 500;

    private final AlerteRepository alerteRepository;
    private final AnamneseRepository anamneseRepository;
    private final MedicamentRepository medicamentRepository;
    private final ScanMedicamentRepository scanRepository;
    private final PartageDonneesRepository partageRepository;
    private final ApplicationEventPublisher evenements;
    /**
     * (Re)calcule les alertes d'un scan à partir de l'anamnèse du patient.
     * Les alertes NON traitées du scan sont remplacées ; celles déjà traitées par le médecin
     * sont conservées (traçabilité) et ne sont pas recréées en double.
     */
    @Transactional
    public List<AlerteResponse> genererPourScan(ScanMedicament scan) {
        alerteRepository.deleteByScanIdAndTraiteeFalse(scan.getId());
        List<Alerte> dejaTraitees = alerteRepository.findByScanIdOrderByDateGenerationDesc(scan.getId());

        Long patientId = scan.getPatient().getId();
        InteractionRulesEngine.ProfilPatient profil = anamneseRepository.findByPatientId(patientId)
                .map(a -> new InteractionRulesEngine.ProfilPatient(a.getAllergies(), a.getPathologiesChroniques(), a.getTraitementsEnCours()))
                .orElse(null);
        Map<String, Set<String>> index = InteractionRulesEngine.indexer(
                medicamentRepository.findAll().stream().map(AlerteService::versAnalyse).toList());

        Medicament medicament = scan.getMedicament();
        List<InteractionRulesEngine.AlerteDetectee> detectees = InteractionRulesEngine.analyser(
                medicament != null ? versAnalyse(medicament) : null,
                profil, index, scan.getDateExpiration(), LocalDate.now());

        List<Alerte> nouvelles = new ArrayList<>();
        for (InteractionRulesEngine.AlerteDetectee detectee : detectees) {
            String message = tronquer(detectee.message());
            boolean dejaTraitee = dejaTraitees.stream()
                    .anyMatch(a -> a.getType() == detectee.type() && a.getMessage().equals(message));
            if (dejaTraitee) {
                continue;
            }
            Alerte alerte = new Alerte();
            alerte.setPatient(scan.getPatient());
            alerte.setScan(scan);
            alerte.setMedicament(medicament);
            alerte.setType(detectee.type());
            alerte.setNiveauGravite(detectee.gravite());
            alerte.setMessage(message);
            nouvelles.add(alerte);
        }
        alerteRepository.saveAll(nouvelles);

        if (!nouvelles.isEmpty()) {
            log.info("Scan {} (patient {}) : {} alerte(s) générée(s) {}", scan.getId(), patientId, nouvelles.size(),
                    nouvelles.stream().map(a -> a.getType() + "/" + a.getNiveauGravite()).toList());
        }
        // Alertes ÉLEVÉES : notification au patient et à ses médecins, APRÈS validation de la transaction
        long graves = nouvelles.stream().filter(a -> a.getNiveauGravite() == NiveauGravite.ELEVE).count();
        if (graves > 0) {
            evenements.publishEvent(new AlertesGravesEvenement(patientId, (int) graves));
        }
        return nouvelles.stream().map(AlerteResponse::from).toList();
    }

    /** Toutes les alertes du patient. */
    @Transactional(readOnly = true)
    public List<AlerteResponse> alertesPatient(Long patientId) {
        return alerteRepository.findByPatientIdOrderByDateGenerationDesc(patientId).stream()
                .map(AlerteResponse::from).toList();
    }

    /** Alertes d'un scan, uniquement pour son propriétaire (sinon 404). */
    @Transactional(readOnly = true)
    public List<AlerteResponse> alertesScan(Long patientId, Long scanId) {
        if (scanRepository.findByIdAndPatientId(scanId, patientId).isEmpty()) {
            throw new ResourceNotFoundException("Scan introuvable");
        }
        return alerteRepository.findByScanIdOrderByDateGenerationDesc(scanId).stream()
                .map(AlerteResponse::from).toList();
    }

    /**
     * Le médecin indique qu'il a pris connaissance de l'alerte.
     * Exige un consentement ACTIF dont le périmètre couvre les alertes.
     */
    @Transactional
    public AlerteResponse marquerTraitee(Long medecinId, Long patientId, Long alerteId) {
        PartageDonnees partage = partageRepository.findByPatientIdAndMedecinIdAndActifTrue(patientId, medecinId)
                .orElseThrow(() -> new ConsentementRequisException(
                        "Ce patient ne vous a pas donné son consentement pour consulter ses données"));
        if (!partage.couvre(PartageDonnees.ALERTES)) {
            throw new ConsentementRequisException("Ce patient ne partage pas ses alertes avec vous");
        }
        Alerte alerte = alerteRepository.findByIdAndPatientId(alerteId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte introuvable"));

        alerte.marquerTraitee();
        log.info("Alerte {} du patient {} marquée traitée par le médecin {}", alerteId, patientId, medecinId);
        return AlerteResponse.from(alerte);
    }

    private static InteractionRulesEngine.MedicamentAnalyse versAnalyse(Medicament m) {
        return new InteractionRulesEngine.MedicamentAnalyse(m.getNomCommercial(), m.getDci(), m.getContreIndications(), m.getInteractions());
    }

    private static String tronquer(String message) {
        return message.length() <= LONGUEUR_MAX_MESSAGE ? message : message.substring(0, LONGUEUR_MAX_MESSAGE - 1) + "…";
    }
}