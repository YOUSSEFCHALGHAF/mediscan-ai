package com.mediscan.mediscan_ai_backend.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mediscan.mediscan_ai_backend.ia.RapprochementMedicament.Reference;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RapprochementMedicamentTest {

    /** Même contenu que la base de démonstration (02_donnees_test.sql). */
    private static final List<Reference> BASE = List.of(
            new Reference(1L, "Doliprane", "paracétamol", "1000 mg"),
            new Reference(2L, "Dafalgan", "paracétamol", "500 mg"),
            new Reference(3L, "Advil", "ibuprofène", "400 mg"),
            new Reference(4L, "Aspirine UPSA", "acide acétylsalicylique", "500 mg"),
            new Reference(5L, "Kardégic", "acide acétylsalicylique", "75 mg"),
            new Reference(6L, "Clamoxyl", "amoxicilline", "1 g"),
            new Reference(7L, "Augmentin", "amoxicilline, acide clavulanique", "1 g / 125 mg"),
            new Reference(8L, "Coumadine", "warfarine", "5 mg"),
            new Reference(9L, "Glucophage", "metformine", "850 mg"),
            new Reference(10L, "Ventoline", "salbutamol", "100 µg/dose"),
            new Reference(11L, "Amlor", "amlodipine", "5 mg"),
            new Reference(12L, "Spasfon", "phloroglucinol", "80 mg"));

    private static Long reconnaitre(String texte) {
        var correspondance = RapprochementMedicament.trouver(MedicamentTextParser.analyser(texte), BASE);
        return correspondance == null ? null : correspondance.reference().id();
    }

    @Test
    @DisplayName("Reconnaît un nom lu parfaitement")
    void nomExact() {
        assertThat(reconnaitre("DOLIPRANE 1000 mg")).isEqualTo(1L);
    }

    @Test
    @DisplayName("Tolère les confusions chiffre/lettre et une lettre fausse")
    void erreursDeLecture() {
        assertThat(reconnaitre("D0LIPRANE")).isEqualTo(1L);
        assertThat(reconnaitre("DOLIPRAME 1000mg")).isEqualTo(1L);
        assertThat(reconnaitre("ADVlL 400 mg")).isEqualTo(3L);
    }

    @Test
    @DisplayName("Le dosage départage deux médicaments de même substance")
    void dosageDepartage() {
        assertThat(reconnaitre("Paracetamol 500 mg gélules")).isEqualTo(2L);   // Dafalgan
        assertThat(reconnaitre("PARACETAMOL 1000 mg")).isEqualTo(1L);          // Doliprane
    }

    @Test
    @DisplayName("Reconnaît un nom en deux mots")
    void nomEnDeuxMots() {
        assertThat(reconnaitre("ASPIRINE UPSA 500 mg")).isEqualTo(4L);
    }

    @Test
    @DisplayName("Ignore les accents")
    void sansAccent() {
        assertThat(reconnaitre("KARDEGIC 75 mg")).isEqualTo(5L);
    }

    @Test
    @DisplayName("Ne reconnaît rien dans un texte sans nom de médicament")
    void texteSansMedicament() {
        assertThat(reconnaitre("Lire attentivement la notice. Conserver hors de portée")).isNull();
    }

    @Test
    @DisplayName("Ne confond pas un médicament absent de la base avec un autre")
    void medicamentAbsent() {
        assertThat(reconnaitre("SMECTA diosmectite 3 g")).isNull();
    }
}