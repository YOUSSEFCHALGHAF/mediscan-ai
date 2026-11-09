package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.dto.response.AlerteATraiterResponse;
import com.mediscan.mediscan_ai_backend.dto.response.TableauDeBordMedecinResponse;
import com.mediscan.mediscan_ai_backend.entity.enums.NiveauGravite;
import com.mediscan.mediscan_ai_backend.entity.enums.TypeAlerte;
import com.mediscan.mediscan_ai_backend.util.StatistiquesTableauDeBord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prérequis : database/02_donnees_test.sql exécuté.
 * Dr Mansour : consentement actif de Marc (ANAMNESE,SCANS,ALERTES,RAPPELS), consentement de Nadia RÉVOQUÉ.
 * Dr Dubois  : consentement actif de Yasmine (ANAMNESE,SCANS,ALERTES : pas les rappels).
 */
@SpringBootTest(properties = {"app.firebase.enabled=false", "app.rappels.planificateur.enabled=false"})
@Transactional
class TableauDeBordMedecinServiceIntegrationTests {

    private static final long DR_MANSOUR = 1L;
    private static final long DR_DUBOIS = 2L;
    private static final LocalDate LE_4_NOVEMBRE = LocalDate.of(2026, 11, 4);

    @Autowired private TableauDeBordMedecinService service;

    @Test
    @DisplayName("Indicateurs du Dr Mansour : 1 patient, 2 alertes dont 1 élevée, 1 rappel actif")
    void indicateurs() {
        TableauDeBordMedecinResponse.Indicateurs i = service.tableauDeBord(DR_MANSOUR, LE_4_NOVEMBRE).indicateurs();

        assertThat(i.patientsSuivis()).isEqualTo(1);
        assertThat(i.alertesNonTraitees()).isEqualTo(2);
        assertThat(i.alertesElevees()).isEqualTo(1);
        assertThat(i.scansSemaine()).isZero();     // dernier scan de Marc : le 18 octobre
        assertThat(i.rappelsActifs()).isEqualTo(1); // Coumadine du soir
    }

    @Test
    @DisplayName("Évolution sur 30 jours : les deux alertes de Marc apparaissent le 18 octobre")
    void evolution() {
        TableauDeBordMedecinResponse tdb = service.tableauDeBord(DR_MANSOUR, LE_4_NOVEMBRE);

        assertThat(tdb.evolutionAlertes()).hasSize(30);
        assertThat(tdb.evolutionAlertes()).contains(new StatistiquesTableauDeBord.PointJour(LocalDate.of(2026, 10, 18), 1, 1, 0));
        assertThat(tdb.repartitionParType()).containsExactly(
                new StatistiquesTableauDeBord.CompteType(TypeAlerte.INTERACTION, 1),
                new StatistiquesTableauDeBord.CompteType(TypeAlerte.CONTRE_INDICATION, 1),
                new StatistiquesTableauDeBord.CompteType(TypeAlerte.ALLERGIE, 0),
                new StatistiquesTableauDeBord.CompteType(TypeAlerte.EXPIRATION, 0));
    }

    @Test
    @DisplayName("Les données de Nadia (consentement RÉVOQUÉ) n'entrent dans aucune statistique")
    void donneesRevoqueesExclues() {
        // Le 20 octobre, Nadia a scanné du Glucophage ; Marc avait scanné de l'Advil le 18.
        TableauDeBordMedecinResponse tdb = service.tableauDeBord(DR_MANSOUR, LocalDate.of(2026, 10, 20));

        assertThat(tdb.indicateurs().scansSemaine()).isEqualTo(1);
        assertThat(tdb.medicamentsLesPlusScannes())
                .extracting(TableauDeBordMedecinResponse.MedicamentScanne::nomCommercial)
                .containsExactly("Advil");
    }

    @Test
    @DisplayName("La file est triée par gravité et indique le patient")
    void fileTriee() {
        var file = service.tableauDeBord(DR_MANSOUR, LE_4_NOVEMBRE).alertesATraiter();

        assertThat(file).extracting(AlerteATraiterResponse::niveauGravite)
                .containsExactly(NiveauGravite.ELEVE, NiveauGravite.MOYEN);
        assertThat(file.get(0).patientId()).isEqualTo(4L);
        assertThat(file.get(0).patientNom()).contains("Marc");
    }

    @Test
    @DisplayName("La page Alertes renvoie toutes les alertes non traitées du médecin")
    void pageAlertes() {
        assertThat(service.alertesATraiter(DR_MANSOUR)).hasSize(2);
        assertThat(service.alertesATraiter(DR_DUBOIS)).isEmpty(); // l'alerte de Yasmine est déjà traitée
    }

    @Test
    @DisplayName("Dr Dubois : le périmètre partagé par Yasmine est respecté (pas de rappels)")
    void perimetreRespecte() {
        TableauDeBordMedecinResponse tdb = service.tableauDeBord(DR_DUBOIS, LocalDate.of(2026, 10, 25));

        assertThat(tdb.indicateurs().patientsSuivis()).isEqualTo(1);
        assertThat(tdb.indicateurs().rappelsActifs()).isZero();
        assertThat(tdb.indicateurs().scansSemaine()).isEqualTo(1); // scan flou du 22 octobre
        assertThat(tdb.medicamentsLesPlusScannes())               // le scan non reconnu n'est pas classé
                .extracting(TableauDeBordMedecinResponse.MedicamentScanne::nomCommercial)
                .containsExactly("Doliprane");
    }

    @Test
    @DisplayName("Les statistiques comptent aussi les alertes déjà traitées (l'historique)")
    void historiqueCompte() {
        TableauDeBordMedecinResponse tdb = service.tableauDeBord(DR_DUBOIS, LocalDate.of(2026, 10, 25));

        assertThat(tdb.indicateurs().alertesNonTraitees()).isZero();
        assertThat(tdb.repartitionParType()).contains(new StatistiquesTableauDeBord.CompteType(TypeAlerte.ALLERGIE, 1));
    }
}