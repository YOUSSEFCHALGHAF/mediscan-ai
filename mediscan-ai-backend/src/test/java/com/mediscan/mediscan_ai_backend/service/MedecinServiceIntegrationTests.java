package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.dto.response.FichePatientResponse;
import com.mediscan.mediscan_ai_backend.dto.response.PatientResumeResponse;
import com.mediscan.mediscan_ai_backend.exception.ConsentementRequisException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prérequis : database/02_donnees_test.sql exécuté. */
@SpringBootTest
@Transactional
class MedecinServiceIntegrationTests {

    private static final long DR_MANSOUR = 1L;
    private static final long DR_DUBOIS = 2L;
    private static final long YASMINE = 3L;
    private static final long MARC = 4L;
    private static final long NADIA = 5L;

    @Autowired private MedecinService medecinService;

    @Test
    @DisplayName("Le Dr Mansour ne voit que Marc, avec 2 alertes non traitées")
    void listePatients() {
        List<PatientResumeResponse> patients = medecinService.mesPatients(DR_MANSOUR);

        assertThat(patients).hasSize(1);
        assertThat(patients.get(0).nom()).isEqualTo("Lefèvre");
        assertThat(patients.get(0).alertesNonTraitees()).isEqualTo(2);
    }

    @Test
    @DisplayName("Fiche de Marc : toutes les catégories partagées sont présentes")
    void ficheComplete() {
        FichePatientResponse fiche = medecinService.fichePatient(DR_MANSOUR, MARC);

        assertThat(fiche.anamnese().traitementsEnCours()).contains("warfarine");
        assertThat(fiche.scans()).hasSize(1);
        assertThat(fiche.alertes()).hasSize(2);
        assertThat(fiche.rappels()).hasSize(1);
    }

    @Test
    @DisplayName("Consentement révoqué (Nadia) : accès refusé")
    void consentementRevoque() {
        assertThatThrownBy(() -> medecinService.fichePatient(DR_MANSOUR, NADIA))
                .isInstanceOf(ConsentementRequisException.class);
    }

    @Test
    @DisplayName("Périmètre respecté : le Dr Dubois ne reçoit pas les rappels de Yasmine")
    void perimetreRespecte() {
        FichePatientResponse fiche = medecinService.fichePatient(DR_DUBOIS, YASMINE);

        assertThat(fiche.anamnese()).isNotNull();
        assertThat(fiche.scans()).isNotNull();
        assertThat(fiche.alertes()).isNotNull();
        assertThat(fiche.rappels()).isNull();
    }
}