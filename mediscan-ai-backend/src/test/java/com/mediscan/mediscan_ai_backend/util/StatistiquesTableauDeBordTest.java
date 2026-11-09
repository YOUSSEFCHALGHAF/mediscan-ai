package com.mediscan.mediscan_ai_backend.util;


import com.mediscan.mediscan_ai_backend.entity.enums.NiveauGravite;
import com.mediscan.mediscan_ai_backend.entity.enums.TypeAlerte;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StatistiquesTableauDeBordTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 10, 6);

    @Test
    @DisplayName("La série compte 30 jours consécutifs, du début au 4 novembre")
    void serieComplete() {
        List<StatistiquesTableauDeBord.PointJour> serie = StatistiquesTableauDeBord.evolution(List.of(), DEBUT, 30);

        assertThat(serie).hasSize(30);
        assertThat(serie.get(0).jour()).isEqualTo(DEBUT);
        assertThat(serie.get(29).jour()).isEqualTo(LocalDate.of(2026, 11, 4));
        assertThat(serie).allSatisfy(p -> assertThat(p.total()).isZero());
    }

    @Test
    @DisplayName("Les comptes sont placés au bon jour ; les jours hors période sont ignorés")
    void comptesParJour() {
        List<StatistiquesTableauDeBord.PointJour> serie = StatistiquesTableauDeBord.evolution(List.of(
                new StatistiquesTableauDeBord.CompteJour(LocalDate.of(2026, 10, 18), NiveauGravite.ELEVE, 1),
                new StatistiquesTableauDeBord.CompteJour(LocalDate.of(2026, 10, 18), NiveauGravite.MOYEN, 1),
                new StatistiquesTableauDeBord.CompteJour(LocalDate.of(2026, 9, 1), NiveauGravite.ELEVE, 9)), DEBUT, 30);

        assertThat(serie.get(12)).isEqualTo(new StatistiquesTableauDeBord.PointJour(LocalDate.of(2026, 10, 18), 1, 1, 0));
        assertThat(serie.stream().mapToLong(StatistiquesTableauDeBord.PointJour::total).sum()).isEqualTo(2);
    }

    @Test
    @DisplayName("La répartition contient toujours les 4 types, dans le même ordre")
    void repartition() {
        assertThat(StatistiquesTableauDeBord.repartition(Map.of(TypeAlerte.ALLERGIE, 2L)))
                .containsExactly(
                        new StatistiquesTableauDeBord.CompteType(TypeAlerte.INTERACTION, 0),
                        new StatistiquesTableauDeBord.CompteType(TypeAlerte.CONTRE_INDICATION, 0),
                        new StatistiquesTableauDeBord.CompteType(TypeAlerte.ALLERGIE, 2),
                        new StatistiquesTableauDeBord.CompteType(TypeAlerte.EXPIRATION, 0));
    }

    @Test
    @DisplayName("La file place les plus graves d'abord, puis les plus récentes")
    void ordreDeLaFile() {
        record A(String nom, NiveauGravite gravite, LocalDateTime date) {
        }
        List<A> file = new ArrayList<>(List.of(
                new A("faible récente", NiveauGravite.FAIBLE, LocalDateTime.of(2026, 11, 3, 10, 0)),
                new A("élevée ancienne", NiveauGravite.ELEVE, LocalDateTime.of(2026, 10, 1, 10, 0)),
                new A("élevée récente", NiveauGravite.ELEVE, LocalDateTime.of(2026, 11, 1, 10, 0)),
                new A("moyenne", NiveauGravite.MOYEN, LocalDateTime.of(2026, 10, 15, 10, 0))));

        file.sort(StatistiquesTableauDeBord.parGraviteEtDate(A::gravite, A::date));

        assertThat(file).extracting(A::nom)
                .containsExactly("élevée récente", "élevée ancienne", "moyenne", "faible récente");
    }
}