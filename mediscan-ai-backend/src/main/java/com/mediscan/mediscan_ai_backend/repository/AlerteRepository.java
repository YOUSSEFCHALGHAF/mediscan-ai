package com.mediscan.mediscan_ai_backend.repository;

import com.mediscan.mediscan_ai_backend.entity.Alerte;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlerteRepository extends JpaRepository<Alerte, Long> {

    @EntityGraph(attributePaths = "medicament")
    List<Alerte> findByPatientIdOrderByDateGenerationDesc(Long patientId);

    List<Alerte> findByPatientIdAndTraiteeFalseOrderByDateGenerationDesc(Long patientId);

    long countByPatientIdAndTraiteeFalse(Long patientId);

    /**
     * Alertes non traitées de TOUS les patients qui ont donné
     * un consentement actif au médecin (dashboard médecin).
     */
    @Query("""
            SELECT a FROM Alerte a
            JOIN FETCH a.patient
            WHERE a.traitee = false
              AND a.patient.id IN (
                  SELECT pa.patient.id FROM PartageDonnees pa
                  WHERE pa.medecin.id = :medecinId AND pa.actif = true)
            ORDER BY a.dateGeneration DESC
            """)
    List<Alerte> findAlertesNonTraiteesPourMedecin(@Param("medecinId") Long medecinId);

    @Query("""
            SELECT COUNT(a) FROM Alerte a
            WHERE a.traitee = false
              AND a.patient.id IN (
                  SELECT pa.patient.id FROM PartageDonnees pa
                  WHERE pa.medecin.id = :medecinId AND pa.actif = true)
            """)
    long countAlertesNonTraiteesPourMedecin(@Param("medecinId") Long medecinId);
}