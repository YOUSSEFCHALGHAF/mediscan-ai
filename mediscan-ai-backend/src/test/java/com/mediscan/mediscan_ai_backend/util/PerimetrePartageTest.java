package com.mediscan.mediscan_ai_backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PerimetrePartageTest {

    @Test
    @DisplayName("Normalise : ordre canonique, sans doublon, insensible à la casse")
    void normalisation() {
        assertThat(PerimetrePartage.normaliser(List.of("rappels", "scans", "ANAMNESE", "scans")))
                .isEqualTo("ANAMNESE,SCANS,RAPPELS");
        assertThat(PerimetrePartage.normaliser(List.of(" alertes "))).isEqualTo("ALERTES");
    }

    @Test
    @DisplayName("Refuse un périmètre vide")
    void videRefuse() {
        assertThatThrownBy(() -> PerimetrePartage.normaliser(List.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("Choisissez");
        assertThatThrownBy(() -> PerimetrePartage.normaliser(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Refuse une catégorie inconnue")
    void inconnueRefusee() {
        assertThatThrownBy(() -> PerimetrePartage.normaliser(List.of("SCANS", "MOT_DE_PASSE")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("MOT_DE_PASSE");
        assertThatThrownBy(() -> PerimetrePartage.normaliser(Arrays.asList("SCANS", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Découpe un périmètre stocké, dans l'ordre canonique")
    void decoupage() {
        assertThat(PerimetrePartage.decouper("RAPPELS, anamnese")).containsExactly("ANAMNESE", "RAPPELS");
        assertThat(PerimetrePartage.decouper("")).isEmpty();
    }
}