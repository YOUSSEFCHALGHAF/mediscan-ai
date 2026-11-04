package com.mediscan.mediscan_ai_backend.ia;

import java.util.Arrays;
import java.util.List;

/**
 * Retrouve le médicament de la base de référence correspondant au texte lu par l'OCR,
 * en tolérant les erreurs de lecture grâce à la distance d'édition (Levenshtein).
 */
public final class RapprochementMedicament {

    /** Similarité minimale pour accepter une correspondance. */
    public static final double SEUIL_ACCEPTATION = 0.75;

    /** Médicament de la base, réduit aux informations utiles pour la comparaison. */
    public record Reference(Long id, String nomCommercial, String dci, String dosage) {
    }

    /** Médicament retenu et score de similarité (0 à 1). */
    public record Correspondance(Reference reference, double score) {
    }

    private RapprochementMedicament() {
    }

    /**
     * @return la meilleure correspondance, ou null si aucun médicament n'atteint le seuil
     */
    public static Correspondance trouver(MedicamentTextParser.InfosExtraites infos, List<Reference> references) {
        Correspondance meilleure = null;
        for (Reference ref : references) {
            double score = score(infos, ref);
            if (score >= SEUIL_ACCEPTATION && (meilleure == null || score > meilleure.score())) {
                meilleure = new Correspondance(ref, Math.min(1.0, score));
            }
        }
        return meilleure;
    }

    static double score(MedicamentTextParser.InfosExtraites infos, Reference ref) {
        String nom = SimilariteTexte.normaliser(ref.nomCommercial());
        String premierMot = nom.split(" ")[0];

        double meilleur = 0;
        for (String candidat : infos.motsCandidats()) {
            // 1. Nom commercial complet ("aspirine upsa"), puis son premier mot ("aspirine")
            meilleur = Math.max(meilleur, SimilariteTexte.similarite(candidat, nom));
            if (!premierMot.equals(nom) && premierMot.length() >= 4) {
                meilleur = Math.max(meilleur, 0.95 * SimilariteTexte.similarite(candidat, premierMot));
            }
            // 2. Substance(s) active(s) : un peu moins sûr (plusieurs marques pour une même DCI)
            for (String dci : Arrays.stream(ref.dci().split(",")).map(SimilariteTexte::normaliser).toList()) {
                if (dci.length() >= 4) {
                    meilleur = Math.max(meilleur, 0.9 * SimilariteTexte.similarite(candidat, dci));
                }
            }
        }

        // 3. Le dosage départage les présentations d'une même substance (Doliprane 1000 / Dafalgan 500)
        Double dosageLu = infos.dosageEnMg();
        Double dosageRef = MedicamentTextParser.dosageEnMilligrammes(ref.dosage());
        if (dosageLu != null && dosageRef != null && meilleur > 0) {
            meilleur += Math.abs(dosageLu - dosageRef) < 0.001 ? 0.05 : -0.05;
        }
        return meilleur;
    }
}