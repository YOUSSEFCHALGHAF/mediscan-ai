package com.mediscan.mediscan_ai_backend.repository;

import com.mediscan.mediscan_ai_backend.entity.ScanMedicament;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScanMedicamentRepository extends JpaRepository<ScanMedicament, Long> {

    /** Historique des scans, du plus récent au plus ancien, avec le médicament chargé. */
    @EntityGraph(attributePaths = "medicament")
    List<ScanMedicament> findByPatientIdOrderByDateScanDesc(Long patientId);

    /** Sécurité : un patient ne peut lire que SES scans. */
    Optional<ScanMedicament> findByIdAndPatientId(Long id, Long patientId);

    long countByPatientId(Long patientId);
}