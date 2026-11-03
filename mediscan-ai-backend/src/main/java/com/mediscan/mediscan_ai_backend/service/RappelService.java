package com.mediscan.mediscan_ai_backend.service;

import com.mediscan.mediscan_ai_backend.dto.request.RappelRequest;
import com.mediscan.mediscan_ai_backend.dto.response.RappelResponse;
import com.mediscan.mediscan_ai_backend.entity.Medicament;
import com.mediscan.mediscan_ai_backend.entity.Rappel;
import com.mediscan.mediscan_ai_backend.exception.ResourceNotFoundException;
import com.mediscan.mediscan_ai_backend.repository.MedicamentRepository;
import com.mediscan.mediscan_ai_backend.repository.PatientRepository;
import com.mediscan.mediscan_ai_backend.repository.RappelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD des rappels de prise. Un patient ne manipule QUE ses rappels. */
@Service
@RequiredArgsConstructor
public class RappelService {

    private final RappelRepository rappelRepository;
    private final MedicamentRepository medicamentRepository;
    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public List<RappelResponse> lister(Long patientId) {
        return rappelRepository.findByPatientIdOrderByHeurePriseAsc(patientId)
                .stream().map(RappelResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RappelResponse lire(Long patientId, Long rappelId) {
        return RappelResponse.from(trouverRappel(patientId, rappelId));
    }

    @Transactional
    public RappelResponse creer(Long patientId, RappelRequest req) {
        Rappel rappel = new Rappel();
        rappel.setPatient(patientRepository.getReferenceById(patientId));
        appliquer(rappel, req);
        rappel.setActif(req.actif() == null || req.actif());
        return RappelResponse.from(rappelRepository.save(rappel));
    }

    @Transactional
    public RappelResponse modifier(Long patientId, Long rappelId, RappelRequest req) {
        Rappel rappel = trouverRappel(patientId, rappelId);
        boolean heureModifiee = !req.heurePrise().equals(rappel.getHeurePrise());
        appliquer(rappel, req);
        if (req.actif() != null) {
            rappel.setActif(req.actif());
        }
        if (heureModifiee) {
            rappel.setDerniereNotification(null); // le rappel pourra être renvoyé à la nouvelle heure
        }
        return RappelResponse.from(rappel);
    }

    @Transactional
    public void supprimer(Long patientId, Long rappelId) {
        rappelRepository.delete(trouverRappel(patientId, rappelId));
    }

    // ---------- utilitaires ----------

    private void appliquer(Rappel rappel, RappelRequest req) {
        Medicament medicament = medicamentRepository.findById(req.medicamentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Médicament introuvable : id " + req.medicamentId()));
        rappel.setMedicament(medicament);
        rappel.setLibelle(req.libelle().trim());
        // Les secondes sont mises à 0 : le planificateur compare à la minute près
        rappel.setHeurePrise(req.heurePrise().withSecond(0).withNano(0));
        rappel.setFrequence(req.frequence() != null ? req.frequence() : Rappel.QUOTIDIEN);
        rappel.setDateDebut(req.dateDebut());
        rappel.setDateFin(req.dateFin());
    }

    /** 404 si le rappel n'existe pas OU appartient à un autre patient. */
    private Rappel trouverRappel(Long patientId, Long rappelId) {
        return rappelRepository.findByIdAndPatientId(rappelId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Rappel introuvable"));
    }
}