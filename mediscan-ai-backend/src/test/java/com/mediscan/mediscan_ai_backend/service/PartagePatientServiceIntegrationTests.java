package com.mediscan.mediscan_ai_backend.service;

import com.mediscan.mediscan_ai_backend.dto.request.PartageRequest;
import com.mediscan.mediscan_ai_backend.dto.request.PerimetreRequest;
import com.mediscan.mediscan_ai_backend.dto.response.MedecinPublicResponse;
import com.mediscan.mediscan_ai_backend.dto.response.PartageResponse;
import com.mediscan.mediscan_ai_backend.dto.response.PatientResumeResponse;
import com.mediscan.mediscan_ai_backend.event.PartageAccordeEvenement;
import com.mediscan.mediscan_ai_backend.exception.ConflitException;
import com.mediscan.mediscan_ai_backend.exception.ConsentementRequisException;
import com.mediscan.mediscan_ai_backend.exception.RequeteInvalideException;
import com.mediscan.mediscan_ai_backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prérequis : database/02_donnees_test.sql exécuté.
 * Partages de démonstration : n°1 Yasmine -> Dr Dubois (actif), n°2 Marc -> Dr Mansour (actif, tout),
 * n°3 Nadia -> Dr Mansour (RÉVOQUÉ).
 */
@SpringBootTest(properties = {"app.firebase.enabled=false", "app.rappels.planificateur.enabled=false"})
@Transactional
@RecordApplicationEvents
class PartagePatientServiceIntegrationTests {

    private static final long DR_MANSOUR = 1L;
    private static final long DR_DUBOIS = 2L;
    private static final long YASMINE = 3L;
    private static final long MARC = 4L;
    private static final long NADIA = 5L;
    private static final long PARTAGE_MARC_MANSOUR = 2L;
    private static final long PARTAGE_NADIA_REVOQUE = 3L;

    @Autowired private PartagePatientService partageService;
    @Autowired private MedecinService medecinService;
    @Autowired private TableauDeBordMedecinService tableauDeBordService;
    @Autowired private ApplicationEvents evenements;

    @Test
    @DisplayName("Recherche par spécialité, par nom complet, sans tenir compte des accents")
    void recherche() {
        assertThat(partageService.rechercherMedecins("card")).extracting(MedecinPublicResponse::nom).containsExactly("Mansour");
        assertThat(partageService.rechercherMedecins("generale")).extracting(MedecinPublicResponse::nom).containsExactly("Dubois");
        assertThat(partageService.rechercherMedecins("claire dub")).extracting(MedecinPublicResponse::nom).containsExactly("Dubois");
        assertThatThrownBy(() -> partageService.rechercherMedecins("%")).isInstanceOf(RequeteInvalideException.class);
    }

    @Test
    @DisplayName("L'historique contient aussi les accès révoqués, avec leur date")
    void historique() {
        List<PartageResponse> partages = partageService.lister(NADIA);

        assertThat(partages).singleElement().satisfies(p -> {
            assertThat(p.actif()).isFalse();
            assertThat(p.dateRevocation()).isNotNull();
            assertThat(p.categories()).containsExactly("ANAMNESE", "SCANS");
        });
    }

    @Test
    @DisplayName("Donner accès : le médecin voit le patient immédiatement, et il est notifié")
    void creation() {
        PartageResponse partage = partageService.creer(NADIA, new PartageRequest(DR_DUBOIS, List.of("alertes", "ANAMNESE")));

        assertThat(partage.actif()).isTrue();
        assertThat(partage.categories()).containsExactly("ANAMNESE", "ALERTES");
        assertThat(medecinService.mesPatients(DR_DUBOIS)).extracting(PatientResumeResponse::id).contains(NADIA);
        assertThat(evenements.stream(PartageAccordeEvenement.class))
                .singleElement().satisfies(e -> assertThat(e.medecinId()).isEqualTo(DR_DUBOIS));
    }

    @Test
    @DisplayName("Un seul accès actif par médecin (409)")
    void pasDeDoublon() {
        assertThatThrownBy(() -> partageService.creer(MARC, new PartageRequest(DR_MANSOUR, List.of("SCANS"))))
                .isInstanceOf(ConflitException.class);
    }

    @Test
    @DisplayName("Redonner accès après une révocation : nouvelle ligne, l'ancienne reste dans l'historique")
    void nouvelAccesApresRevocation() {
        partageService.creer(NADIA, new PartageRequest(DR_MANSOUR, List.of("ANAMNESE")));

        List<PartageResponse> partages = partageService.lister(NADIA);
        assertThat(partages).hasSize(2);
        assertThat(partages).filteredOn(PartageResponse::actif).hasSize(1);
    }

    @Test
    @DisplayName("Réduire le périmètre : nouveau consentement, ancien révoqué, et la fiche du médecin se réduit")
    void modificationPerimetre() {
        PartageResponse nouveau = partageService.modifierPerimetre(MARC, PARTAGE_MARC_MANSOUR, new PerimetreRequest(List.of("ANAMNESE")));

        assertThat(nouveau.id()).isNotEqualTo(PARTAGE_MARC_MANSOUR);
        assertThat(nouveau.categories()).containsExactly("ANAMNESE");
        assertThat(partageService.lister(MARC)).filteredOn(p -> p.id().equals(PARTAGE_MARC_MANSOUR))
                .singleElement().satisfies(p -> assertThat(p.actif()).isFalse());

        // Le médecin ne voit plus les alertes de Marc, ni sur la fiche, ni dans ses statistiques
        assertThat(medecinService.fichePatient(DR_MANSOUR, MARC).alertes()).isNull();
        assertThat(tableauDeBordService.tableauDeBord(DR_MANSOUR, LocalDate.of(2026, 11, 4))
                .indicateurs().alertesNonTraitees()).isZero();
    }

    @Test
    @DisplayName("Même périmètre : aucune nouvelle ligne dans l'historique")
    void modificationSansChangement() {
        PartageResponse resultat = partageService.modifierPerimetre(MARC, PARTAGE_MARC_MANSOUR,
                new PerimetreRequest(List.of("RAPPELS", "ALERTES", "SCANS", "ANAMNESE")));

        assertThat(resultat.id()).isEqualTo(PARTAGE_MARC_MANSOUR);
        assertThat(partageService.lister(MARC)).hasSize(1);
    }

    @Test
    @DisplayName("BOUT EN BOUT : la révocation retire le patient de la liste, de la fiche et des statistiques")
    void revocationImmediate() {
        partageService.revoquer(MARC, PARTAGE_MARC_MANSOUR);

        assertThat(medecinService.mesPatients(DR_MANSOUR)).isEmpty();
        assertThatThrownBy(() -> medecinService.fichePatient(DR_MANSOUR, MARC))
                .isInstanceOf(ConsentementRequisException.class);
        assertThat(tableauDeBordService.tableauDeBord(DR_MANSOUR, LocalDate.of(2026, 11, 4))
                .indicateurs().patientsSuivis()).isZero();

        partageService.revoquer(MARC, PARTAGE_MARC_MANSOUR); // une seconde révocation ne pose pas de problème
    }

    @Test
    @DisplayName("Un patient ne touche pas aux partages d'un autre, ni à un accès déjà retiré")
    void securite() {
        assertThatThrownBy(() -> partageService.revoquer(YASMINE, PARTAGE_MARC_MANSOUR))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> partageService.modifierPerimetre(NADIA, PARTAGE_NADIA_REVOQUE, new PerimetreRequest(List.of("SCANS"))))
                .isInstanceOf(ConflitException.class);
        assertThatThrownBy(() -> partageService.creer(NADIA, new PartageRequest(DR_DUBOIS, List.of("TOUT"))))
                .isInstanceOf(RequeteInvalideException.class);
    }
}