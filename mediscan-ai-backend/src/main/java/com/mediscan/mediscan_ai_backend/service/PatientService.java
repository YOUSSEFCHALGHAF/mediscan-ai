package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.dto.request.ProfilPatientRequest;
import com.mediscan.mediscan_ai_backend.dto.response.ScanResponse;
import com.mediscan.mediscan_ai_backend.dto.response.UtilisateurResponse;
import com.mediscan.mediscan_ai_backend.entity.Patient;
import com.mediscan.mediscan_ai_backend.exception.ResourceNotFoundException;
import com.mediscan.mediscan_ai_backend.repository.PatientRepository;
import com.mediscan.mediscan_ai_backend.repository.ScanMedicamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Profil du patient et historique de ses scans. */
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final ScanMedicamentRepository scanRepository;

    @Transactional(readOnly = true)
    public UtilisateurResponse profil(Long patientId) {
        return UtilisateurResponse.from(trouverPatient(patientId));
    }

    @Transactional
    public UtilisateurResponse modifierProfil(Long patientId, ProfilPatientRequest req) {
        Patient p = trouverPatient(patientId);
        p.setNom(req.nom().trim());
        p.setPrenom(req.prenom().trim());
        p.setDateNaissance(req.dateNaissance());
        p.setSexe(req.sexe());
        p.setGroupeSanguin(req.groupeSanguin());
        p.setTelephone(req.telephone());
        // Pas de save() nécessaire : l'entité est suivie par JPA, les changements
        // sont enregistrés automatiquement à la fin de la transaction.
        return UtilisateurResponse.from(p);
    }

    @Transactional(readOnly = true)
    public List<ScanResponse> historiqueScans(Long patientId) {
        return scanRepository.findByPatientIdOrderByDateScanDesc(patientId)
                .stream().map(ScanResponse::from).toList();
    }

    /** Un patient ne voit QUE ses scans : sinon 404 (on ne révèle pas l'existence du scan). */
    @Transactional(readOnly = true)
    public ScanResponse scan(Long patientId, Long scanId) {
        return scanRepository.findByIdAndPatientId(scanId, patientId)
                .map(ScanResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Scan introuvable"));
    }

    private Patient trouverPatient(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient introuvable"));
    }
}