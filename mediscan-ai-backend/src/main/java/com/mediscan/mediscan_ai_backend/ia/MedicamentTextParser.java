package com.mediscan.mediscan_ai_backend.ia;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrait les informations utiles du texte brut lu par l'OCR sur une boîte de médicament :
 * mots candidats pour le nom, dosage, numéro de lot et date d'expiration.
 * Les expressions tolèrent les erreurs fréquentes de lecture (O/0, I/1, espaces parasites).
 */
public final class MedicamentTextParser {

    /** Résultat de l'analyse du texte. */
    public record InfosExtraites(
            List<String> motsCandidats,
            String dosage,
            Double dosageEnMg,
            String numeroLot,
            LocalDate dateExpiration) {
    }

    // "1000 mg", "1OOO mg", "500mg", "1 g", "2,5 mg", "100 µg", "5 ml"
    private static final Pattern DOSAGE = Pattern.compile(
            "(?<![\\p{L}\\d])([0-9OoIl]*[0-9][0-9OoIl]*(?:[.,][0-9OoIl]+)?)\\s?(mg|g|µg|ug|mcg|ml|ui)(?![\\p{L}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // "LOT 24B117", "Lot: 5K2290", "L0T. G85A31", "N° de lot 24B117"
    private static final Pattern LOT = Pattern.compile(
            "\\b(?:N°\\s*(?:de\\s*)?)?L[O0]T\\s*(?:N°)?\\s*[:.]?\\s*([A-Z0-9]{4,12})\\b",
            Pattern.CASE_INSENSITIVE);

    // "EXP 03/2028", "EXP : 03-28", "Exp. 3.2028", "PER 11/2027", "Utiliser avant 06/2027"
    private static final Pattern EXPIRATION_MOT_CLE = Pattern.compile(
            "\\b(?:EXP(?:IRY|IRATION)?|P[EÉ]R(?:EMPTION)?|UTILISER\\s+AVANT|USE\\s+BY)\\b\\.?\\s*[:.]?\\s*"
                    + "(?:(\\d{1,2})\\s*[/.\\-]\\s*)?(\\d{1,2})\\s*[/.\\-]\\s*(\\d{4}|\\d{2})\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // Sans mot-clé : "03/2028" seul (mois/année sur 4 chiffres uniquement, pour éviter les faux positifs)
    private static final Pattern MOIS_ANNEE = Pattern.compile("\\b(0[1-9]|1[0-2])\\s*[/\\-]\\s*(20\\d{2})\\b");

    private static final Pattern MOT = Pattern.compile("[\\p{L}0-9|]+");

    /** Mots fréquents sur les boîtes, inutiles pour identifier le médicament. */
    private static final Set<String> MOTS_IGNORES = Set.of(
            "comprime", "comprimes", "gelule", "gelules", "boite", "voie", "orale", "adulte", "adultes",
            "enfant", "enfants", "sachet", "sachets", "flacon", "pellicule", "pellicules", "secable",
            "effervescent", "effervescents", "dispersible", "dispersibles", "enrobe", "enrobes",
            "medicament", "notice", "lire", "attentivement", "conserver", "portee", "temperature",
            "exp", "lot", "avant", "utiliser", "tablets", "film", "coated", "pour", "avec", "sans",
            "dans", "plus", "ans", "solution", "buvable", "poudre", "suspension", "inhalation");

    private MedicamentTextParser() {
    }

    public static InfosExtraites analyser(String texteBrut) {
        String texte = texteBrut == null ? "" : texteBrut;
        String dosage = null;
        Double dosageEnMg = null;
        Matcher m = DOSAGE.matcher(texte);
        if (m.find()) {
            String nombre = corrigerChiffres(m.group(1)).replace(',', '.');
            String unite = normaliserUnite(m.group(2));
            try {
                double valeur = Double.parseDouble(nombre);
                dosage = formaterNombre(valeur) + " " + unite;
                dosageEnMg = enMilligrammes(valeur, unite);
            } catch (NumberFormatException ignore) {
                // dosage illisible : on l'ignore
            }
        }
        return new InfosExtraites(motsCandidats(texte), dosage, dosageEnMg, numeroLot(texte), dateExpiration(texte));
    }

    /** Mots (et paires de mots consécutifs) pouvant correspondre au nom commercial ou à la DCI. */
    static List<String> motsCandidats(String texte) {
        List<String> mots = new ArrayList<>();
        Matcher m = MOT.matcher(texte);
        while (m.find()) {
            String brut = m.group();
            String normalise = SimilariteTexte.normaliser(brut);
            long lettres = brut.chars().filter(Character::isLetter).count();
            if (normalise.length() >= 4 && lettres >= 3 && !MOTS_IGNORES.contains(normalise)) {
                mots.add(normalise);
            }
        }
        Set<String> candidats = new LinkedHashSet<>(mots);
        for (int i = 0; i + 1 < mots.size(); i++) {
            candidats.add(mots.get(i) + " " + mots.get(i + 1));
        }
        return new ArrayList<>(candidats);
    }

    static String numeroLot(String texte) {
        Matcher m = LOT.matcher(texte);
        while (m.find()) {
            String valeur = m.group(1).toUpperCase(Locale.ROOT);
            if (valeur.chars().anyMatch(Character::isDigit)) {
                return valeur;
            }
        }
        return null;
    }

    static LocalDate dateExpiration(String texte) {
        Matcher m = EXPIRATION_MOT_CLE.matcher(texte);
        if (m.find()) {
            LocalDate date = construireDate(m.group(1), m.group(2), m.group(3));
            if (date != null) {
                return date;
            }
        }
        Matcher sansMotCle = MOIS_ANNEE.matcher(texte);
        if (sansMotCle.find()) {
            return construireDate(null, sansMotCle.group(1), sansMotCle.group(2));
        }
        return null;
    }

    /** Une date "mois/année" correspond au DERNIER jour du mois (le médicament est utilisable tout le mois). */
    private static LocalDate construireDate(String jour, String mois, String annee) {
        try {
            int a = Integer.parseInt(annee);
            if (a < 100) {
                a += 2000;
            }
            int mo = Integer.parseInt(mois);
            if (a < 2000 || a > 2100 || mo < 1 || mo > 12) {
                return null;
            }
            if (jour != null) {
                int j = Integer.parseInt(jour);
                YearMonth ym = YearMonth.of(a, mo);
                return j >= 1 && j <= ym.lengthOfMonth() ? ym.atDay(j) : ym.atEndOfMonth();
            }
            return YearMonth.of(a, mo).atEndOfMonth();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String corrigerChiffres(String nombre) {
        return nombre.replace('O', '0').replace('o', '0').replace('I', '1').replace('l', '1');
    }

    private static String normaliserUnite(String unite) {
        String u = unite.toLowerCase(Locale.ROOT);
        return switch (u) {
            case "ug", "mcg", "µg" -> "µg";
            case "ml" -> "ml";
            case "ui" -> "UI";
            default -> u;
        };
    }

    private static Double enMilligrammes(double valeur, String unite) {
        return switch (unite) {
            case "mg" -> valeur;
            case "g" -> valeur * 1000;
            case "µg" -> valeur / 1000;
            default -> null;
        };
    }

    private static String formaterNombre(double valeur) {
        return valeur == Math.rint(valeur) ? String.valueOf((long) valeur) : String.valueOf(valeur).replace('.', ',');
    }

    /** Convertit un dosage de la base ("1 g / 125 mg", "1000 mg") en milligrammes (première valeur). */
    public static Double dosageEnMilligrammes(String dosage) {
        if (dosage == null) {
            return null;
        }
        return analyser(dosage).dosageEnMg();
    }
}