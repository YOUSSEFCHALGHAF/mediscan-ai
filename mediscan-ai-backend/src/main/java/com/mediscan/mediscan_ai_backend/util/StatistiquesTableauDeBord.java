package com.mediscan.mediscan_ai_backend.util;

import com.mediscan.mediscan_ai_backend.entity.enums.NiveauGravite;
import com.mediscan.mediscan_ai_backend.entity.enums.TypeAlerte;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Mise en forme des statistiques du tableau de bord médecin (Java pur, testable).
 * Les requêtes SQL ne renvoient que les jours et les types qui ont des alertes :
 * ces méthodes complètent avec des zéros pour obtenir des graphiques continus.
 */
public final class StatistiquesTableauDeBord {

    /** Résultat brut d'une agrégation : nombre d'alertes d'une gravité donnée, un jour donné. */
    public record CompteJour(LocalDate jour, NiveauGravite gravite, long nombre) {
    }

    /** Un point du graphique d'évolution : alertes du jour, par gravité. */
    public record PointJour(LocalDate jour, long elevees, long moyennes, long faibles) {
        public long total() {
            return elevees + moyennes + faibles;
        }
    }

    /** Nombre d'alertes d'un type donné. */
    public record CompteType(TypeAlerte type, long nombre) {
    }

    private StatistiquesTableauDeBord() {
    }

    /**
     * Série quotidienne de nbJours points à partir de "debut", y compris les jours sans alerte (zéros).
     * Les lignes en dehors de la période sont ignorées.
     */
    public static List<PointJour> evolution(List<CompteJour> lignes, LocalDate debut, int nbJours) {
        Map<LocalDate, Map<NiveauGravite, Long>> parJour = new HashMap<>();
        for (CompteJour ligne : lignes) {
            parJour.computeIfAbsent(ligne.jour(), j -> new EnumMap<>(NiveauGravite.class))
                    .merge(ligne.gravite(), ligne.nombre(), Long::sum);
        }
        List<PointJour> serie = new ArrayList<>(nbJours);
        for (int i = 0; i < nbJours; i++) {
            LocalDate jour = debut.plusDays(i);
            Map<NiveauGravite, Long> comptes = parJour.getOrDefault(jour, Map.of());
            serie.add(new PointJour(jour,
                    comptes.getOrDefault(NiveauGravite.ELEVE, 0L),
                    comptes.getOrDefault(NiveauGravite.MOYEN, 0L),
                    comptes.getOrDefault(NiveauGravite.FAIBLE, 0L)));
        }
        return serie;
    }

    /** Les 4 types d'alerte, toujours dans le même ordre, avec 0 pour les types absents. */
    public static List<CompteType> repartition(Map<TypeAlerte, Long> comptes) {
        return Arrays.stream(TypeAlerte.values())
                .map(type -> new CompteType(type, comptes.getOrDefault(type, 0L)))
                .toList();
    }

    /** Ordre de la file des alertes : la plus grave d'abord, puis la plus récente. */
    public static <T> Comparator<T> parGraviteEtDate(Function<T, NiveauGravite> gravite,
                                                     Function<T, LocalDateTime> date) {
        return Comparator.comparing(gravite, Comparator.reverseOrder())
                .thenComparing(date, Comparator.reverseOrder());
    }
}