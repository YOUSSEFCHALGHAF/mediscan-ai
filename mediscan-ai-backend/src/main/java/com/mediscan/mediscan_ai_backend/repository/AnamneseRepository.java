package com.mediscan.mediscan_ai_backend.repository;


import com.mediscan.mediscan_ai_backend.entity.Anamnese;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnamneseRepository extends JpaRepository<Anamnese, Long> {

    Optional<Anamnese> findByPatientId(Long patientId);

    boolean existsByPatientId(Long patientId);
}