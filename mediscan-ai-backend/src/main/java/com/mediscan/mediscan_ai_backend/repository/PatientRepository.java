package com.mediscan.mediscan_ai_backend.repository;

import com.mediscan.mediscan_ai_backend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByEmailIgnoreCase(String email);

    /**
     * Patients qu'un médecin a le droit de voir :
     * uniquement ceux qui lui ont donné un consentement ACTIF.
     * (Le service de partage empêche d'avoir deux partages actifs identiques.)
     */
    @Query("""
            SELECT pa.patient FROM PartageDonnees pa
            WHERE pa.medecin.id = :medecinId
              AND pa.actif = true
            ORDER BY pa.patient.nom, pa.patient.prenom
            """)
    List<Patient> findPatientsAvecConsentementActif(@Param("medecinId") Long medecinId);
}