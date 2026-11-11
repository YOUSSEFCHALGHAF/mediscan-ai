package com.mediscan.mediscan_ai_backend.repository;

import com.mediscan.mediscan_ai_backend.entity.Medecin;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedecinRepository extends JpaRepository<Medecin, Long> {

    Optional<Medecin> findByEmailIgnoreCase(String email);

    boolean existsByNumeroRpps(String numeroRpps);

    /** Liste des médecins proposés au patient pour le partage. */
    List<Medecin> findByActifTrueOrderByNomAscPrenomAsc();
    /**
     * Recherche d'un médecin par nom, prénom, "prénom nom" ou spécialité.
     * La collation utf8mb4_unicode_ci (étape 3) rend la comparaison insensible
     * aux majuscules ET aux accents : "generale" trouve "Médecine générale".
     */
    @Query("""
            SELECT m FROM Medecin m
            WHERE m.actif = true AND (
                   m.nom LIKE CONCAT('%', :q, '%')
                OR m.prenom LIKE CONCAT('%', :q, '%')
                OR m.specialite LIKE CONCAT('%', :q, '%')
                OR CONCAT(m.prenom, ' ', m.nom) LIKE CONCAT('%', :q, '%')
                OR CONCAT(m.nom, ' ', m.prenom) LIKE CONCAT('%', :q, '%'))
            ORDER BY m.nom, m.prenom
            """)
    List<Medecin> rechercher(@Param("q") String q, Pageable limite);
}