package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.dto.response.AlerteResponse;
import com.mediscan.mediscan_ai_backend.entity.ScanMedicament;
import com.mediscan.mediscan_ai_backend.entity.enums.NiveauGravite;
import com.mediscan.mediscan_ai_backend.entity.enums.TypeAlerte;
import com.mediscan.mediscan_ai_backend.exception.ConsentementRequisException;
import com.mediscan.mediscan_ai_backend.exception.ResourceNotFoundException;
import com.mediscan.mediscan_ai_backend.repository.ScanMedicamentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prérequis : database/02_donnees_test.sql exécuté.
 * Chaque test est annulé à la fin (@Transactional) : la base reste intacte.
 */
@SpringBootTest
@Transactional
class AlerteServiceIntegrationTests {

    private static final long DR_MANSOUR = 1L;   // consentement de Marc (avec ALERTES), révoqué par Nadia
    private static final long DR_DUBOIS = 2L;    // consentement de Yasmine (avec ALERTES)
    private static final long YASMINE = 3L;
    private static final long MARC = 4L;
    private static final long NADIA = 5L;
    private static final long SCAN_ADVIL_MARC = 2L;
    private static final long SCAN_FLOU_YASMINE = 4L;
    private static final long CLAMOXYL = 6L;

    @Autowired private AlerteService alerteService;
    @Autowired private ScanService scanService;
    @Autowired private ScanMedicamentRepository scanRepository;

    @Test
    @DisplayName("Scan Advil de Marc : le moteur produit l'interaction ÉLEVÉE avec la warfarine")
    void interactionMarc() {
        ScanMedicament scan = scanRepository.findById(SCAN_ADVIL_MARC).orElseThrow();

        List<AlerteResponse> alertes = alerteService.genererPourScan(scan);

        assertThat(alertes).extracting(AlerteResponse::type).containsExactly(TypeAlerte.INTERACTION);
        assertThat(alertes.get(0).niveauGravite()).isEqualTo(NiveauGravite.ELEVE);
        assertThat(alertes.get(0).message()).contains("warfarine");
        // Les alertes non traitées de démonstration ont été REMPLACÉES : pas de doublon
        assertThat(alerteService.alertesScan(MARC, SCAN_ADVIL_MARC)).hasSize(1);
    }

    @Test
    @DisplayName("Correction du scan de Yasmine en Clamoxyl : alerte d'ALLERGIE ÉLEVÉE")
    void correctionDeclencheAllergie() {
        scanService.corrigerMedicament(YASMINE, SCAN_FLOU_YASMINE, CLAMOXYL);

        List<AlerteResponse> alertes = alerteService.alertesScan(YASMINE, SCAN_FLOU_YASMINE);
        assertThat(alertes).anySatisfy(a -> {
            assertThat(a.type()).isEqualTo(TypeAlerte.ALLERGIE);
            assertThat(a.niveauGravite()).isEqualTo(NiveauGravite.ELEVE);
            assertThat(a.traitee()).isFalse();
        });
        // L'alerte de démonstration déjà traitée par le médecin est conservée
        assertThat(alertes).anySatisfy(a -> assertThat(a.traitee()).isTrue());
    }

    @Test
    @DisplayName("Une alerte traitée par le médecin n'est pas recréée lors d'un recalcul")
    void pasDeDoublonApresTraitement() {
        ScanMedicament scan = scanRepository.findById(SCAN_ADVIL_MARC).orElseThrow();
        AlerteResponse alerte = alerteService.genererPourScan(scan).get(0);
        alerteService.marquerTraitee(DR_MANSOUR, MARC, alerte.id());

        List<AlerteResponse> recalcul = alerteService.genererPourScan(scan);

        assertThat(recalcul).isEmpty();
        assertThat(alerteService.alertesScan(MARC, SCAN_ADVIL_MARC)).hasSize(1)
                .allSatisfy(a -> assertThat(a.traitee()).isTrue());
    }

    @Test
    @DisplayName("Le médecin avec consentement marque une alerte comme traitée")
    void medecinTraiteAlerte() {
        Long id = premiereNonTraitee(MARC);

        AlerteResponse resultat = alerteService.marquerTraitee(DR_MANSOUR, MARC, id);

        assertThat(resultat.traitee()).isTrue();
    }

    @Test
    @DisplayName("Refus sans consentement actif (Dr Dubois -> Marc, Dr Mansour -> Nadia révoqué)")
    void refusSansConsentement() {
        Long id = premiereNonTraitee(MARC);

        assertThatThrownBy(() -> alerteService.marquerTraitee(DR_DUBOIS, MARC, id))
                .isInstanceOf(ConsentementRequisException.class);
        assertThatThrownBy(() -> alerteService.marquerTraitee(DR_MANSOUR, NADIA, id))
                .isInstanceOf(ConsentementRequisException.class);
    }

    @Test
    @DisplayName("Une alerte d'un autre patient est introuvable (404), même avec un consentement valide")
    void alerteDUnAutrePatient() {
        Long alerteDeMarc = premiereNonTraitee(MARC);

        assertThatThrownBy(() -> alerteService.marquerTraitee(DR_DUBOIS, YASMINE, alerteDeMarc))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Un patient ne voit pas les alertes du scan d'un autre patient")
    void alertesScanDUnAutre() {
        assertThatThrownBy(() -> alerteService.alertesScan(YASMINE, SCAN_ADVIL_MARC))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Long premiereNonTraitee(Long patientId) {
        return alerteService.alertesPatient(patientId).stream()
                .filter(a -> !a.traitee()).findFirst().orElseThrow().id();
    }
}