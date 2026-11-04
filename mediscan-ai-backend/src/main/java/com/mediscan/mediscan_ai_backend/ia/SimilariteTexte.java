package com.mediscan.mediscan_ai_backend.ia;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Outils de comparaison de texte tolérants aux erreurs de l'OCR.
 */
public final class SimilariteTexte {

    private SimilariteTexte() {
    }

    /**
     * Minuscules, sans accents, et correction des confusions typiques de l'OCR
     * entre chiffres et lettres : "D0LIPRANE" → "doliprane", "C1AMOXYL" → "ciamoxyl".
     */
    public static String normaliser(String texte) {
        if (texte == null) {
            return "";
        }
        String sansAccents = Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase(Locale.ROOT)
                .replace('0', 'o')
                .replace('1', 'i')
                .replace('5', 's')
                .replace('|', 'l')
                .replaceAll("[^a-z ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Distance de Levenshtein : nombre minimal de lettres à ajouter, supprimer ou remplacer. */
    public static int distance(String a, String b) {
        int[] precedente = new int[b.length() + 1];
        int[] courante = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            precedente[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            courante[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cout = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                courante[j] = Math.min(Math.min(courante[j - 1] + 1, precedente[j] + 1), precedente[j - 1] + cout);
            }
            int[] echange = precedente;
            precedente = courante;
            courante = echange;
        }
        return precedente[b.length()];
    }

    /** Similarité entre 0 (rien en commun) et 1 (identiques), sur des textes déjà normalisés. */
    public static double similarite(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        return 1.0 - (double) distance(a, b) / Math.max(a.length(), b.length());
    }
}