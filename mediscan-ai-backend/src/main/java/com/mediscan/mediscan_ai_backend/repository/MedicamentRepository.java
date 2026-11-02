package com.mediscan.mediscan_ai_backend.repository;

import com.mediscan.mediscan_ai_backend.entity.Medicament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicamentRepository extends JpaRepository<Medicament, Long> {

    List<Medicament> findByNomCommercialIgnoreCase(String nomCommercial);

    Optional<Medicament> findFirstByNomCommercialIgnoreCaseAndDosageIgnoreCase(String nom, String dosage);

    List<Medicament> findByDciContainingIgnoreCase(String dci);

    /**
     * Recherche par nom commercial OU par DCI.
     * Grâce à la collation utf8mb4_unicode_ci de MySQL, la recherche ignore
     * les accents et la casse : "IBUPROFENE" trouve "ibuprofène" (testé).
     */
    @Query("""
            SELECT m FROM Medicament m
            WHERE LOWER(m.nomCommercial) LIKE LOWER(CONCAT('%', :terme, '%'))
               OR LOWER(m.dci)           LIKE LOWER(CONCAT('%', :terme, '%'))
            ORDER BY m.nomCommercial
            """)
    List<Medicament> rechercher(@Param("terme") String terme);
}