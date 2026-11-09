package com.mediscan.mediscan_ai_backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class PlanificationRappelTest {

    private static PlanificationRappel.Creneau creneau(String heure, String frequence, String debut, String fin,
                                                       boolean actif, String derniere) {
        return new PlanificationRappel.Creneau(LocalTime.parse(heure), frequence, LocalDate.parse(debut),
                fin == null ? null : LocalDate.parse(fin), actif,
                derniere == null ? null : LocalDateTime.parse(derniere));
    }

    private static boolean du(PlanificationRappel.Creneau c, String maintenant) {
        return PlanificationRappel.estDu(c, LocalDateTime.parse(maintenant));
    }

    private static final PlanificationRappel.Creneau SOIR = creneau("20:00", "QUOTIDIEN", "2026-09-12", null, true, null);

    @Test
    @DisplayName("Envoyé quand l'heure arrive, pas avant")
    void heureArrivee() {
        assertThat(du(SOIR, "2026-11-04T20:00:00")).isTrue();
        assertThat(du(SOIR, "2026-11-04T19:59:00")).isFalse();
    }

    @Test
    @DisplayName("Tolérance de 15 minutes après un retard du serveur")
    void tolerance() {
        assertThat(du(SOIR, "2026-11-04T20:04:00")).isTrue();
        assertThat(du(SOIR, "2026-11-04T20:20:00")).isFalse();
    }

    @Test
    @DisplayName("Pas de doublon le même jour, mais renvoyé le lendemain")
    void pasDeDoublon() {
        assertThat(du(creneau("20:00", "QUOTIDIEN", "2026-09-12", null, true, "2026-11-04T20:00:01"),
                "2026-11-04T20:01:00")).isFalse();
        assertThat(du(creneau("20:00", "QUOTIDIEN", "2026-09-12", null, true, "2026-11-03T20:00:01"),
                "2026-11-04T20:00:30")).isTrue();
    }

    @Test
    @DisplayName("Respecte l'activation et la période")
    void periode() {
        assertThat(du(creneau("20:00", "QUOTIDIEN", "2026-09-12", null, false, null), "2026-11-04T20:00:00")).isFalse();
        assertThat(du(creneau("20:00", "QUOTIDIEN", "2026-12-01", null, true, null), "2026-11-04T20:00:00")).isFalse();
        assertThat(du(creneau("20:00", "QUOTIDIEN", "2026-09-01", "2026-10-31", true, null), "2026-11-04T20:00:00")).isFalse();
        assertThat(du(creneau("20:00", "QUOTIDIEN", "2026-09-01", "2026-11-04", true, null), "2026-11-04T20:00:00")).isTrue();
    }

    @Test
    @DisplayName("Hebdomadaire : le même jour de la semaine que la date de début")
    void hebdomadaire() {
        PlanificationRappel.Creneau mercredi = creneau("09:00", "HEBDOMADAIRE", "2026-10-07", null, true, null);
        assertThat(du(mercredi, "2026-11-04T09:00:00")).isTrue();   // mercredi
        assertThat(du(mercredi, "2026-11-05T09:00:00")).isFalse();  // jeudi
    }

    @Test
    @DisplayName("Ponctuel : uniquement le jour J")
    void ponctuel() {
        PlanificationRappel.Creneau unique = creneau("12:00", "PONCTUEL", "2026-11-04", null, true, null);
        assertThat(du(unique, "2026-11-04T12:00:00")).isTrue();
        assertThat(du(unique, "2026-11-05T12:00:00")).isFalse();
    }

    @Test
    @DisplayName("Passage de minuit : un rappel de 23:58 vérifié à 00:03 est envoyé une seule fois")
    void passageMinuit() {
        assertThat(du(creneau("23:58", "QUOTIDIEN", "2026-09-01", null, true, "2026-11-03T23:58:00"),
                "2026-11-05T00:03:00")).isTrue();
        assertThat(du(creneau("23:58", "QUOTIDIEN", "2026-09-01", null, true, "2026-11-04T23:58:05"),
                "2026-11-05T00:03:00")).isFalse();
    }
}