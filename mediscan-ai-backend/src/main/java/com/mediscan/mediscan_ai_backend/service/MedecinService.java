package com.mediscan.mediscan_ai_backend.service;

import com.mediscan.mediscan_ai_backend.dto.response.*;
import com.mediscan.mediscan_ai_backend.entity.PartageDonnees;
import com.mediscan.mediscan_ai_backend.entity.Patient;
import com.mediscan.mediscan_ai_backend.exception.ConsentementRequisException;
import com.mediscan.mediscan_ai_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Accès du médecin aux données de ses patients.
 * RÈGLE CENTRALE : aucun accès sans consentement ACTIF du patient,
 * et uniquement aux catégories autorisées (perimetreDonnees).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedecinService {

    private final PartageDonneesRepository partageRepository;
    private final AnamneseRepository anamneseRepository;
    private final ScanMedicamentRepository scanRepository;
    private final AlerteRepository alerteRepository;
    private final RappelRepository rappelRepository;

    /** Patients ayant donné un consentement actif, triés par nom. */
    @Transactional(readOnly = true)
    public List<PatientResumeResponse> mesPatients(Long medecinId) {
        return partageRepository.findByMedecinIdAndActifTrue(medecinId).stream()
                .map(partage -> PatientResumeResponse.from(partage,
                        alerteRepository.countByPatientIdAndTraiteeFalse(partage.getPatient().getId())))
                .sorted(Comparator.comparing(PatientResumeResponse::nom)
                        .thenComparing(PatientResumeResponse::prenom))
                .toList();
    }

    /** Fiche patient : 403 si pas de consentement actif. */
    @Transactional(readOnly = true)
    public FichePatientResponse fichePatient(Long medecinId, Long patientId) {
        PartageDonnees partage = partageRepository
                .findByPatientIdAndMedecinIdAndActifTrue(patientId, medecinId)
                .orElseThrow(() -> {
                    log.warn("Accès refusé : médecin {} -> patient {} (pas de consentement actif)",
                            medecinId, patientId);
                    return new ConsentementRequisException(
                            "Ce patient ne vous a pas donné son consentement pour consulter ses données");
                });

        // Traçabilité : chaque consultation est journalisée
        log.info("Consultation de la fiche du patient {} par le médecin {} (périmètre : {})",
                patientId, medecinId, partage.getPerimetreDonnees());

        Patient p = partage.getPatient();

        AnamneseResponse anamnese = partage.couvre(PartageDonnees.ANAMNESE)
                ? anamneseRepository.findByPatientId(patientId)
                .map(AnamneseResponse::from).orElseGet(AnamneseResponse::vide)
                : null;

        List<ScanResponse> scans = partage.couvre(PartageDonnees.SCANS)
                ? scanRepository.findByPatientIdOrderByDateScanDesc(patientId)
                .stream().map(ScanResponse::from).toList()
                : null;

        List<AlerteResponse> alertes = partage.couvre(PartageDonnees.ALERTES)
                ? alerteRepository.findByPatientIdOrderByDateGenerationDesc(patientId)
                .stream().map(AlerteResponse::from).toList()
                : null;

        List<RappelResponse> rappels = partage.couvre(PartageDonnees.RAPPELS)
                ? rappelRepository.findByPatientIdOrderByHeurePriseAsc(patientId)
                .stream().map(RappelResponse::from).toList()
                : null;

        return new FichePatientResponse(p.getId(), p.getNom(), p.getPrenom(), p.getEmail(),
                p.getTelephone(), p.getDateNaissance(),
                PatientResumeResponse.calculerAge(p.getDateNaissance()),
                p.getSexe(), p.getGroupeSanguin(),
                partage.getPerimetreDonnees(), partage.getDateConsentement(),
                anamnese, scans, alertes, rappels);
    }
}