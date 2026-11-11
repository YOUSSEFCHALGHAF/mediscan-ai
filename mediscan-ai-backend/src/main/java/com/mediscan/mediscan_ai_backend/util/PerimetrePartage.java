package com.mediscan.mediscan_ai_backend.util;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Règles du périmètre d'un partage (Java pur, testable)  – uniquement les 4 catégories connues  – au moins une catégorie (un partage vide n'aurait aucun sens)  – sans doublon, toujours dans le même ordre : "ANAMNESE,SCANS,ALERTES,RAPPELS".
 * L'ordre canonique permet de comparer deux périmètres par simple égalité de texte.
 */
public final class PerimetrePartage {

    /** Catégories possibles, dans l'ordre canonique (mêmes valeurs que PartageDonnees). */
    public static final List<String> CATEGORIES = List.of("ANAMNESE", "SCANS", "ALERTES", "RAPPELS");

    private PerimetrePartage() {
    }

    /**
     * @return le périmètre normalisé, ex. ["scans", "ANAMNESE", "scans"] → "ANAMNESE,SCANS"
     * @throws IllegalArgumentException si une catégorie est inconnue ou si aucune n'est choisie
     */
    public static String normaliser(Collection<String> categories) {
        if (categories == null || categories.isEmpty()) {
            throw new IllegalArgumentException("Choisissez au moins une catégorie de données à partager");
        }
        Set<String> choisies = new LinkedHashSet<>();
        for (String categorie : categories) {
            String c = categorie == null ? "" : categorie.trim().toUpperCase(Locale.ROOT);
            if (!CATEGORIES.contains(c)) {
                throw new IllegalArgumentException("Catégorie inconnue : " + categorie
                        + " (valeurs possibles : " + String.join(", ", CATEGORIES) + ")");
            }
            choisies.add(c);
        }
        return String.join(",", CATEGORIES.stream().filter(choisies::contains).toList());
    }

    /** "ANAMNESE,SCANS" → ["ANAMNESE", "SCANS"] (dans l'ordre canonique). */
    public static List<String> decouper(String perimetre) {
        if (perimetre == null || perimetre.isBlank()) {
            return List.of();
        }
        Set<String> presentes = new LinkedHashSet<>();
        for (String c : perimetre.split(",")) {
            presentes.add(c.trim().toUpperCase(Locale.ROOT));
        }
        return CATEGORIES.stream().filter(presentes::contains).toList();
    }
}