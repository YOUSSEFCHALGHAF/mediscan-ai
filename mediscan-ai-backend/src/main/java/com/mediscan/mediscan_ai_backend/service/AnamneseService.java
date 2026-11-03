package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.dto.request.AnamneseRequest;
import com.mediscan.mediscan_ai_backend.dto.response.AnamneseResponse;
import com.mediscan.mediscan_ai_backend.entity.Anamnese;
import com.mediscan.mediscan_ai_backend.repository.AnamneseRepository;
import com.mediscan.mediscan_ai_backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lecture et enregistrement de l'anamnèse (une seule par patient). */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnamneseService {

    private final AnamneseRepository anamneseRepository;
    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public AnamneseResponse lire(Long patientId) {
        return anamneseRepository.findByPatientId(patientId)
                .map(AnamneseResponse::from)
                .orElseGet(AnamneseResponse::vide);
    }

    /** Crée l'anamnèse si elle n'existe pas, sinon la met à jour ("upsert"). */
    @Transactional
    public AnamneseResponse enregistrer(Long patientId, AnamneseRequest req) {
        Anamnese anamnese = anamneseRepository.findByPatientId(patientId)
                .orElseGet(() -> {
                    Anamnese nouvelle = new Anamnese();
                    nouvelle.setPatient(patientRepository.getReferenceById(patientId));
                    return nouvelle;
                });

        anamnese.setAntecedents(nettoyer(req.antecedents()));
        anamnese.setAllergies(nettoyer(req.allergies()));
        anamnese.setPathologiesChroniques(nettoyer(req.pathologiesChroniques()));
        anamnese.setTraitementsEnCours(nettoyer(req.traitementsEnCours()));
        anamnese.setHabitudesVie(nettoyer(req.habitudesVie()));

        Anamnese enregistree = anamneseRepository.saveAndFlush(anamnese);
        log.info("Anamnèse enregistrée pour le patient {}", patientId);
        return AnamneseResponse.from(enregistree);
    }

    /** Texte vide ou espaces => null (évite de stocker des chaînes vides). */
    private String nettoyer(String texte) {
        return (texte == null || texte.isBlank()) ? null : texte.trim();
    }
}