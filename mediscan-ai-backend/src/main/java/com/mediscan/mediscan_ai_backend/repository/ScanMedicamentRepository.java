package com.mediscan.mediscan_ai_backend.repository;

import com.mediscan.mediscan_ai_backend.entity.ScanMedicament;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScanMedicamentRepository extends JpaRepository<ScanMedicament, Long> {

    /** Historique des scans, du plus récent au plus ancien, avec le médicament chargé. */
    @EntityGraph(attributePaths = "medicament")
    List<ScanMedicament> findByPatientIdOrderByDateScanDesc(Long patientId);

    /** Sécurité : un patient ne peut lire que SES scans. */
    Optional<ScanMedicament> findByIdAndPatientId(Long id, Long patientId);

    long countByPatientId(Long patientId);
    // ---------- Étape 18 : tableau de bord médecin ----------

    long countByPatientIdInAndDateScanGreaterThanEqual(Collection<Long> patients, LocalDateTime depuis);

    /** Médicaments les plus scannés : [id, nomCommercial, dosage, nombre]. Le Pageable limite le nombre de lignes. */
    @Query("""
            SELECT m.id, m.nomCommercial, m.dosage, COUNT(s) FROM ScanMedicament s
            JOIN s.medicament m
            WHERE s.patient.id IN :patients AND s.dateScan >= :depuis
            GROUP BY m.id, m.nomCommercial, m.dosage
            ORDER BY COUNT(s) DESC, m.nomCommercial
            """)
    List<Object[]> medicamentsLesPlusScannes(@Param("patients") Collection<Long> patients,
                                             @Param("depuis") LocalDateTime depuis,
                                             Pageable limite);
}