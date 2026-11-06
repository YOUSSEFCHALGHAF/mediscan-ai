package com.mediscan.mediscan_ai_backend.repository;

import com.mediscan.mediscan_ai_backend.entity.Alerte;
import com.mediscan.mediscan_ai_backend.entity.ScanMedicament;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlerteRepository extends JpaRepository<Alerte, Long> {

    @EntityGraph(attributePaths = "medicament")
    List<Alerte> findByPatientIdOrderByDateGenerationDesc(Long patientId);

    List<Alerte> findByPatientIdAndTraiteeFalseOrderByDateGenerationDesc(Long patientId);

    long countByPatientIdAndTraiteeFalse(Long patientId);
    long deleteByScan(ScanMedicament scan);
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

    // ---------- Étape 16 ----------

    /** Alertes d'un scan (résultat du scan, détail). */
    @EntityGraph(attributePaths = "medicament")
    List<Alerte> findByScanIdOrderByDateGenerationDesc(Long scanId);

    /** Recalcul des alertes d'un scan : on retire celles que le médecin n'a pas encore traitées. */
    long deleteByScanIdAndTraiteeFalse(Long scanId);

    /** Sécurité : une alerte n'est modifiable que dans le dossier du patient concerné. */
    Optional<Alerte> findByIdAndPatientId(Long id, Long patientId);
}