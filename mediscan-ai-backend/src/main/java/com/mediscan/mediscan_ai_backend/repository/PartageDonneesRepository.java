package com.mediscan.mediscan_ai_backend.repository;

import com.mediscan.mediscan_ai_backend.entity.PartageDonnees;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartageDonneesRepository extends JpaRepository<PartageDonnees, Long> {

    /** LA vérification de sécurité clé : ce médecin a-t-il le droit de voir ce patient ? */
    boolean existsByPatientIdAndMedecinIdAndActifTrue(Long patientId, Long medecinId);

    Optional<PartageDonnees> findByPatientIdAndMedecinIdAndActifTrue(Long patientId, Long medecinId);

    /** Historique complet des consentements d'un patient (actifs et révoqués). */
    @EntityGraph(attributePaths = "medecin")
    List<PartageDonnees> findByPatientIdOrderByDateConsentementDesc(Long patientId);

    @EntityGraph(attributePaths = "patient")
    List<PartageDonnees> findByMedecinIdAndActifTrue(Long medecinId);
    /** Sécurité : un patient ne manipule que SES partages (sinon 404). */
    @EntityGraph(attributePaths = "medecin")
    Optional<PartageDonnees> findByIdAndPatientId(Long id, Long patientId);
}