package com.mediscan.mediscan_ai_backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListeTexteTest {

    @Test
    @DisplayName("Ajoute un élément en fin de liste")
    void ajoutSimple() {
        assertThat(ListeTexte.ajouter("warfarine, amlodipine", "ibuprofène"))
                .isEqualTo("warfarine, amlodipine, ibuprofène");
    }

    @Test
    @DisplayName("N'ajoute pas un doublon, même avec accents ou majuscules différents")
    void pasDeDoublon() {
        assertThat(ListeTexte.ajouter("warfarine, Ibuprofene", "ibuprofène"))
                .isEqualTo("warfarine, Ibuprofene");
    }

    @Test
    @DisplayName("Remplace « aucune » et accepte une liste vide")
    void listeVide() {
        assertThat(ListeTexte.ajouter("aucune", "paracétamol")).isEqualTo("paracétamol");
        assertThat(ListeTexte.ajouter(null, "paracétamol")).isEqualTo("paracétamol");
    }

    @Test
    @DisplayName("Ajoute chaque substance d'une DCI multiple, sans doublon")
    void dciMultiple() {
        assertThat(ListeTexte.ajouter("amoxicilline", "amoxicilline, acide clavulanique"))
                .isEqualTo("amoxicilline, acide clavulanique");
    }

    @Test
    @DisplayName("contientTout exige la présence de TOUTES les substances")
    void contientTout() {
        assertThat(ListeTexte.contientTout("Warfarine, ibuprofene", "ibuprofène")).isTrue();
        assertThat(ListeTexte.contientTout("amoxicilline", "amoxicilline, acide clavulanique")).isFalse();
    }

    @Test
    @DisplayName("Une liste « aucune » ne contient rien")
    void aucuneNeContientRien() {
        assertThat(ListeTexte.contientTout("aucune", "x")).isFalse();
        assertThat(ListeTexte.decouper("aucune")).isEmpty();
    }
}