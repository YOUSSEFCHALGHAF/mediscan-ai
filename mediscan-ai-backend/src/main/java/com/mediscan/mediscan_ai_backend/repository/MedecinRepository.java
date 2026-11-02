package com.mediscan.mediscan_ai_backend.repository;

import com.mediscan.mediscan_ai_backend.entity.Medecin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedecinRepository extends JpaRepository<Medecin, Long> {

    Optional<Medecin> findByEmailIgnoreCase(String email);

    boolean existsByNumeroRpps(String numeroRpps);

    /** Liste des médecins proposés au patient pour le partage. */
    List<Medecin> findByActifTrueOrderByNomAscPrenomAsc();
}