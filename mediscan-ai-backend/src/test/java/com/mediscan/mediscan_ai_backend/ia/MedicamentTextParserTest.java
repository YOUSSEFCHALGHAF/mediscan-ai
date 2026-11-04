package com.mediscan.mediscan_ai_backend.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MedicamentTextParserTest {

    @Test
    @DisplayName("Lit le dosage, le lot et l'expiration d'une boîte nette")
    void boiteNette() {
        var infos = MedicamentTextParser.analyser("DOLIPRANE 1000 mg\nparacétamol\n8 comprimés\nLOT 24B117 EXP 03/2028");

        assertThat(infos.dosage()).isEqualTo("1000 mg");
        assertThat(infos.numeroLot()).isEqualTo("24B117");
        assertThat(infos.dateExpiration()).isEqualTo(LocalDate.of(2028, 3, 31));
    }

    @Test
    @DisplayName("Corrige les erreurs typiques de l'OCR (O/0 dans les chiffres, L0T, année sur 2 chiffres)")
    void erreursOcr() {
        var infos = MedicamentTextParser.analyser("D0LIPRANE 1OOO mg L0T: 24b117 Exp. 03-28");

        assertThat(infos.dosage()).isEqualTo("1000 mg");
        assertThat(infos.numeroLot()).isEqualTo("24B117");
        assertThat(infos.dateExpiration()).isEqualTo(LocalDate.of(2028, 3, 31));
    }

    @Test
    @DisplayName("Convertit les grammes en milligrammes et comprend PER et Lot n°")
    void conversionsEtVariantes() {
        var infos = MedicamentTextParser.analyser("Augmentin 1 g/125 mg sachet PER 11/2027 Lot n° G85A31");

        assertThat(infos.dosageEnMg()).isEqualTo(1000.0);
        assertThat(infos.dateExpiration()).isEqualTo(LocalDate.of(2027, 11, 30));
        assertThat(infos.numeroLot()).isEqualTo("G85A31");
    }

    @Test
    @DisplayName("Lit une date complète et un dosage en microgrammes")
    void dateCompleteEtMicrogrammes() {
        var infos = MedicamentTextParser.analyser("Ventoline 100 µg/dose suspension. Utiliser avant 15/06/2027");

        assertThat(infos.dosage()).isEqualTo("100 µg");
        assertThat(infos.dateExpiration()).isEqualTo(LocalDate.of(2027, 6, 15));
    }

    @Test
    @DisplayName("Accepte un dosage collé et une date mois/année sans mot-clé")
    void dosageColleEtDateSansMotCle() {
        var infos = MedicamentTextParser.analyser("Glucophage 850mg 30 comprimés 06/2027");

        assertThat(infos.dosage()).isEqualTo("850 mg");
        assertThat(infos.dateExpiration()).isEqualTo(LocalDate.of(2027, 6, 30));
    }

    @Test
    @DisplayName("N'invente pas de numéro de lot sans chiffre")
    void pasDeFauxLot() {
        assertThat(MedicamentTextParser.analyser("Spasfon 80 mg Lot SLOGAN pas de numéro").numeroLot()).isNull();
    }

    @Test
    @DisplayName("N'invente pas de date d'expiration impossible")
    void pasDeFausseDate() {
        assertThat(MedicamentTextParser.analyser("Mg magnésium 13/45 texte").dateExpiration()).isNull();
    }

    @Test
    @DisplayName("Produit des mots candidats et des paires, sans les mots courants")
    void motsCandidats() {
        var candidats = MedicamentTextParser.motsCandidats("ASPIRINE UPSA 500 mg comprimés");

        assertThat(candidats).containsExactly("aspirine", "upsa", "aspirine upsa");
    }
}