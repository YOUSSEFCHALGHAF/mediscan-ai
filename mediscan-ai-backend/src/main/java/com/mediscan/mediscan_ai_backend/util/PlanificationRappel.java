package com.mediscan.mediscan_ai_backend.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Décide si un rappel de prise doit être envoyé maintenant (Java pur, testable).
 *
 * Règles  – le rappel est actif, et le jour concerné est dans sa période [dateDebut ; dateFin]  – QUOTIDIEN : tous les jours ; HEBDOMADAIRE : le même jour de la semaine que dateDebut ;
 *    PONCTUEL : uniquement le jour de dateDebut  – l'heure est arrivée, avec une tolérance (si le serveur était arrêté à 08:00, le rappel part à 08:04,
 *    mais pas à 14:00)  – pas de doublon : derniereNotification doit être antérieure à l'échéance.
 * L'échéance de la veille est aussi examinée : un rappel de 23:58 vérifié à 00:01 n'est pas perdu.
 */
public final class PlanificationRappel {

    public static final String QUOTIDIEN = "QUOTIDIEN";
    public static final String HEBDOMADAIRE = "HEBDOMADAIRE";
    public static final String PONCTUEL = "PONCTUEL";

    /** Retard maximal accepté pour envoyer un rappel (panne, redémarrage du serveur...). */
    public static final Duration TOLERANCE = Duration.ofMinutes(15);

    /** Les informations d'un rappel utiles à la décision. */
    public record Creneau(LocalTime heurePrise, String frequence, LocalDate dateDebut, LocalDate dateFin,
                          boolean actif, LocalDateTime derniereNotification) {
    }

    private PlanificationRappel() {
    }

    /** Vrai si le rappel doit être envoyé à l'instant "maintenant". */
    public static boolean estDu(Creneau c, LocalDateTime maintenant) {
        return echeanceDue(c, maintenant) != null;
    }

    /** L'échéance à envoyer (aujourd'hui ou hier), ou null s'il n'y a rien à envoyer. */
    public static LocalDateTime echeanceDue(Creneau c, LocalDateTime maintenant) {
        if (!c.actif() || c.heurePrise() == null || c.dateDebut() == null) {
            return null;
        }
        LocalDate aujourdhui = maintenant.toLocalDate();
        for (LocalDate jour : new LocalDate[]{aujourdhui, aujourdhui.minusDays(1)}) {
            if (!concerneLeJour(c, jour)) {
                continue;
            }
            LocalDateTime echeance = jour.atTime(c.heurePrise());
            boolean arrivee = !echeance.isAfter(maintenant);
            boolean pasTropTard = maintenant.isBefore(echeance.plus(TOLERANCE));
            boolean dejaEnvoye = c.derniereNotification() != null && !c.derniereNotification().isBefore(echeance);
            if (arrivee && pasTropTard && !dejaEnvoye) {
                return echeance;
            }
        }
        return null;
    }

    /** Le rappel s'applique-t-il ce jour-là (période + fréquence) ? */
    static boolean concerneLeJour(Creneau c, LocalDate jour) {
        if (jour.isBefore(c.dateDebut()) || (c.dateFin() != null && jour.isAfter(c.dateFin()))) {
            return false;
        }
        String frequence = c.frequence() == null ? QUOTIDIEN : c.frequence();
        return switch (frequence) {
            case HEBDOMADAIRE -> jour.getDayOfWeek() == c.dateDebut().getDayOfWeek();
            case PONCTUEL -> jour.equals(c.dateDebut());
            default -> true;
        };
    }
}