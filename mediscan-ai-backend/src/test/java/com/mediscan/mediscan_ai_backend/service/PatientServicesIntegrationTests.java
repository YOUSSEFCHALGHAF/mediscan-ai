package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.dto.request.AnamneseRequest;
import com.mediscan.mediscan_ai_backend.dto.request.RappelRequest;
import com.mediscan.mediscan_ai_backend.dto.response.AnamneseResponse;
import com.mediscan.mediscan_ai_backend.dto.response.RappelResponse;
import com.mediscan.mediscan_ai_backend.exception.RequeteInvalideException;
import com.mediscan.mediscan_ai_backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prérequis : database/02_donnees_test.sql exécuté. Chaque test est annulé. */
@SpringBootTest
@Transactional
class PatientServicesIntegrationTests {

    private static final long MARC = 4L;
    private static final long NADIA = 5L;

    @Autowired private AnamneseService anamneseService;
    @Autowired private RappelService rappelService;
    @Autowired private PatientService patientService;
    @Autowired private MedicamentService medicamentService;

    @Test
    @DisplayName("Mise à jour de l'anamnèse : texte nettoyé, champ vide => null")
    void miseAJourAnamnese() {
        AnamneseResponse rep = anamneseService.enregistrer(MARC, new AnamneseRequest(
                "  Infarctus (2019)  ", "aucune", "hypertension", "warfarine", "   "));

        assertThat(rep.antecedents()).isEqualTo("Infarctus (2019)");
        assertThat(rep.habitudesVie()).isNull();
        assertThat(rep.complete()).isTrue();
    }

    @Test
    @DisplayName("Création d'un rappel : actif par défaut, secondes à zéro")
    void creationRappel() {
        RappelResponse rep = rappelService.creer(MARC, new RappelRequest(
                11L, "Amlor du matin", LocalTime.of(8, 30, 45), null,
                LocalDate.of(2026, 11, 2), null, null));

        assertThat(rep.actif()).isTrue();
        assertThat(rep.frequence()).isEqualTo("QUOTIDIEN");
        assertThat(rep.heurePrise()).isEqualTo(LocalTime.of(8, 30));
        assertThat(rappelService.lister(MARC)).hasSize(2);
    }

    @Test
    @DisplayName("Un patient ne peut pas lire le rappel d'un autre patient")
    void rappelAutrePatient() {
        long rappelDeNadia = rappelService.lister(NADIA).get(0).id();

        assertThatThrownBy(() -> rappelService.lire(MARC, rappelDeNadia))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Un patient ne peut pas lire le scan d'un autre patient")
    void scanAutrePatient() {
        assertThatThrownBy(() -> patientService.scan(MARC, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Recherche de médicament : 2 caractères minimum")
    void rechercheTropCourte() {
        assertThatThrownBy(() -> medicamentService.rechercher("a"))
                .isInstanceOf(RequeteInvalideException.class);
        assertThat(medicamentService.rechercher("PARACETAMOL")).hasSize(2);
    }
}