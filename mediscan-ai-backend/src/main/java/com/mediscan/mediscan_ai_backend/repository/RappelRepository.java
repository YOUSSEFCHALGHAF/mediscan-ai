package com.mediscan.mediscan_ai_backend.repository;

import com.mediscan.mediscan_ai_backend.entity.Rappel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RappelRepository extends JpaRepository<Rappel, Long> {

    @EntityGraph(attributePaths = "medicament")
    List<Rappel> findByPatientIdOrderByHeurePriseAsc(Long patientId);

    /** Sécurité : un patient ne peut modifier que SES rappels. */
    Optional<Rappel> findByIdAndPatientId(Long id, Long patientId);

    /**
     * Rappels à envoyer à une heure donnée (appelé chaque minute par le @Scheduled, étape 17) :
     * actifs, déjà commencés, et pas encore terminés.
     */
    @Query("""
            SELECT r FROM Rappel r
            JOIN FETCH r.patient
            JOIN FETCH r.medicament
            WHERE r.actif = true
              AND r.heurePrise = :heure
              AND r.dateDebut <= :aujourdhui
              AND (r.dateFin IS NULL OR r.dateFin >= :aujourdhui)
            """)
    List<Rappel> findRappelsADeclencher(@Param("heure") LocalTime heure,
                                        @Param("aujourdhui") LocalDate aujourdhui);
    /**
     * Rappels actifs dont la période couvre hier ou aujourd'hui (étape 17).
     * PlanificationRappel décide ensuite, rappel par rappel, s'il faut l'envoyer maintenant.
     */
    @Query("""
            SELECT r FROM Rappel r
            JOIN FETCH r.patient
            JOIN FETCH r.medicament
            WHERE r.actif = true
              AND r.dateDebut <= :aujourdhui
              AND (r.dateFin IS NULL OR r.dateFin >= :hier)
            """)
    List<Rappel> findRappelsActifsEntre(@Param("hier") LocalDate hier,
                                        @Param("aujourdhui") LocalDate aujourdhui);
    /** Étape 18 : rappels actifs des patients du médecin. */
    long countByPatientIdInAndActifTrue(Collection<Long> patients);
}