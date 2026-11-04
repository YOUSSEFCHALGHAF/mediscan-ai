package com.mediscan.mediscan_ai_backend.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Manipulation des listes stockées sous forme de texte séparé par des virgules
 * (allergies, traitements en cours...) : "warfarine, amlodipine".
 */
public final class ListeTexte {

    /** Valeurs qui signifient "liste vide". */
    private static final Set<String> VALEURS_VIDES = Set.of("aucun", "aucune", "neant", "rien", "-");

    private ListeTexte() {
    }

    /** "warfarine, amlodipine" -> ["warfarine", "amlodipine"] ("aucune" -> []). */
    public static List<String> decouper(String liste) {
        if (liste == null || liste.isBlank()) {
            return new ArrayList<>();
        }
        List<String> elements = new ArrayList<>();
        for (String element : liste.split(",")) {
            String propre = element.trim();
            if (!propre.isEmpty() && !VALEURS_VIDES.contains(cle(propre))) {
                elements.add(propre);
            }
        }
        return elements;
    }

    /**
     * Ajoute un ou plusieurs éléments ("amoxicilline, acide clavulanique") à la liste,
     * sans doublon : la comparaison ignore les majuscules et les accents.
     *
     * @return la nouvelle liste sous forme de texte (null si elle est vide).
     */
    public static String ajouter(String liste, String elements) {
        List<String> resultat = decouper(liste);
        for (String nouveau : decouper(elements)) {
            if (!contient(resultat, nouveau)) {
                resultat.add(nouveau);
            }
        }
        return resultat.isEmpty() ? null : String.join(", ", resultat);
    }

    /** Vrai si TOUS les éléments sont déjà présents dans la liste. */
    public static boolean contientTout(String liste, String elements) {
        List<String> existants = decouper(liste);
        List<String> recherches = decouper(elements);
        return !recherches.isEmpty() && recherches.stream().allMatch(e -> contient(existants, e));
    }

    private static boolean contient(List<String> liste, String element) {
        String recherche = cle(element);
        return liste.stream().anyMatch(e -> cle(e).equals(recherche));
    }

    /** Clé de comparaison : minuscules, sans accents, espaces normalisés. */
    static String cle(String texte) {
        return Normalizer.normalize(texte, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}