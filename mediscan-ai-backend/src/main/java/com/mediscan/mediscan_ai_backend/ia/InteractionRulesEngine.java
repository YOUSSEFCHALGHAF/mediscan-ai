package com.mediscan.mediscan_ai_backend.ia;

import com.mediscan.mediscan_ai_backend.entity.enums.NiveauGravite;
import com.mediscan.mediscan_ai_backend.entity.enums.TypeAlerte;
import com.mediscan.mediscan_ai_backend.util.ListeTexte;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Moteur de règles de sécurité médicamenteuse (Java pur, sans Spring ni base de données).
 * Croise un médicament avec le profil du patient et produit des alertes :
 *  1. ALLERGIE          : la substance (ou sa famille) correspond à une allergie déclarée → ÉLEVÉE ;
 *  2. INTERACTION       : avec un traitement en cours (dans les deux sens), ou doublon de substance ;
 *  3. CONTRE_INDICATION : une pathologie chronique figure dans les contre-indications ;
 *  4. EXPIRATION        : périmé (ÉLEVÉE) ou expire dans les 3 mois (FAIBLE).
 *
 * AVERTISSEMENT : règles simplifiées à but pédagogique, qui ne remplacent pas un avis médical.
 */
public final class InteractionRulesEngine {

    /** Le médicament analysé (issu de la base de référence). */
    public record MedicamentAnalyse(String nomCommercial, String dci, String contreIndications, String interactions) {
    }

    /** Profil du patient, tel que saisi dans l'anamnèse (listes séparées par des virgules). */
    public record ProfilPatient(String allergies, String pathologiesChroniques, String traitementsEnCours) {
    }

    /** Une alerte détectée, prête à être enregistrée. */
    public record AlerteDetectee(TypeAlerte type, NiveauGravite gravite, String message) {
    }

    /** Familles de substances : une allergie à la famille concerne chacun de ses membres. */
    private static final Map<String, Set<String>> FAMILLES = Map.of(
            "penicilline", Set.of("amoxicilline", "ampicilline", "penicilline", "oxacilline", "cloxacilline",
                    "benzylpenicilline", "phenoxymethylpenicilline", "piperacilline"),
            "cephalosporine", Set.of("cefalexine", "cefuroxime", "ceftriaxone", "cefixime", "cefpodoxime", "cefaclor"),
            "betalactamine", Set.of("amoxicilline", "ampicilline", "penicilline", "cefalexine", "cefuroxime",
                    "ceftriaxone", "cefixime", "cefpodoxime"),
            "aspirine", Set.of("acide acetylsalicylique"),
            "salicyle", Set.of("acide acetylsalicylique"),
            "ains", Set.of("ibuprofene", "ketoprofene", "diclofenac", "naproxene", "acide acetylsalicylique", "celecoxib"),
            "anti-inflammatoire", Set.of("ibuprofene", "ketoprofene", "diclofenac", "naproxene", "celecoxib"),
            "sulfamide", Set.of("sulfamethoxazole", "sulfadiazine"),
            "macrolide", Set.of("clarithromycine", "azithromycine", "erythromycine"));

    /** Substances à marge thérapeutique étroite : toute interaction est jugée ÉLEVÉE. */
    private static final Map<String, String> SUBSTANCES_A_RISQUE = Map.of(
            "warfarine", "risque hémorragique accru",
            "acenocoumarol", "risque hémorragique accru",
            "fluindione", "risque hémorragique accru",
            "apixaban", "risque hémorragique accru",
            "rivaroxaban", "risque hémorragique accru",
            "methotrexate", "risque de toxicité du méthotrexate",
            "lithium", "risque de surdosage en lithium",
            "digoxine", "risque de surdosage en digoxine");

    /** Mots trop généraux pour identifier une pathologie. */
    private static final Set<String> MOTS_VIDES = Set.of(
            "type", "cours", "chronique", "chroniques", "severe", "moderee", "legere", "antecedent", "maladie", "trouble", "aigue");

    private static final DateTimeFormatter MOIS_ANNEE = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private InteractionRulesEngine() {
    }

    /**
     * Applique toutes les règles.
     *
     * @param medicament            médicament identifié (null si non reconnu : seule l'expiration est vérifiée)
     * @param profil                profil du patient (null si pas d'anamnèse)
     * @param interactionsReference pour chaque substance de la base, ses interactions connues (voir indexer)
     * @param dateExpiration        date lue sur la boîte (peut être null)
     * @param aujourdhui            date du jour (paramètre pour rendre les tests reproductibles)
     * @return les alertes, de la plus grave à la moins grave
     */
    public static List<AlerteDetectee> analyser(MedicamentAnalyse medicament, ProfilPatient profil,
                                                Map<String, Set<String>> interactionsReference,
                                                LocalDate dateExpiration, LocalDate aujourdhui) {
        List<AlerteDetectee> alertes = new ArrayList<>();
        if (medicament != null && profil != null) {
            regleAllergie(medicament, profil).ifPresent(alertes::add);
            alertes.addAll(regleInteraction(medicament, profil, interactionsReference));
            alertes.addAll(regleContreIndication(medicament, profil));
        }
        regleExpiration(medicament, dateExpiration, aujourdhui).ifPresent(alertes::add);
        alertes.sort(Comparator.comparing(AlerteDetectee::gravite).reversed());
        return alertes;
    }

    /** Index "substance → substances avec lesquelles elle interagit", construit depuis la base. */
    public static Map<String, Set<String>> indexer(Collection<MedicamentAnalyse> base) {
        Map<String, Set<String>> index = new HashMap<>();
        for (MedicamentAnalyse m : base) {
            Set<String> interactions = cles(m.interactions());
            for (String substance : cles(m.dci())) {
                index.computeIfAbsent(substance, k -> new LinkedHashSet<>()).addAll(interactions);
            }
        }
        return index;
    }

    // ==================== 1. Allergie ====================

    private static Optional<AlerteDetectee> regleAllergie(MedicamentAnalyse m, ProfilPatient p) {
        Set<String> substances = cles(m.dci());
        String nom = cle(m.nomCommercial());
        List<String> allergiesConcernees = new ArrayList<>();

        for (String allergie : ListeTexte.decouper(p.allergies())) {
            String a = singulier(cle(allergie));
            boolean concerne = nom.equals(a)
                    || substances.stream().anyMatch(s -> s.equals(a) || mots(s).contains(a))
                    || FAMILLES.getOrDefault(a, Set.of()).stream().anyMatch(substances::contains);
            if (concerne) {
                allergiesConcernees.add(allergie);
            }
        }
        if (allergiesConcernees.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AlerteDetectee(TypeAlerte.ALLERGIE, NiveauGravite.ELEVE,
                "Allergie déclarée (" + String.join(", ", allergiesConcernees) + ") : " + m.nomCommercial()
                        + " contient " + m.dci() + ". Ne le prenez pas sans l'avis de votre médecin."));
    }

    // ==================== 2. Interaction ====================

    private static List<AlerteDetectee> regleInteraction(MedicamentAnalyse m, ProfilPatient p,
                                                         Map<String, Set<String>> reference) {
        Set<String> substances = cles(m.dci());
        Set<String> interactionsDuMedicament = cles(m.interactions());
        List<AlerteDetectee> alertes = new ArrayList<>();

        for (String traitement : ListeTexte.decouper(p.traitementsEnCours())) {
            String t = cle(traitement);

            // Même substance déjà prise : risque de surdosage (ex. deux médicaments à base de paracétamol).
            if (substances.contains(t)) {
                alertes.add(new AlerteDetectee(TypeAlerte.INTERACTION, NiveauGravite.MOYEN,
                        "Doublon : " + traitement + " fait déjà partie de vos traitements en cours, et "
                                + m.nomCommercial() + " en contient aussi : risque de surdosage."));
                continue;
            }

            // Dans les deux sens : le médicament scanné cite le traitement, OU le traitement cite le médicament
            Set<String> interactionsDuTraitement = reference.getOrDefault(t, Set.of());
            boolean interaction = interactionsDuMedicament.contains(t)
                    || substances.stream().anyMatch(interactionsDuTraitement::contains);
            if (!interaction) {
                continue;
            }

            String substanceScannee = substances.stream().filter(interactionsDuTraitement::contains).findFirst()
                    .orElse(cle(m.dci()));
            String risque = SUBSTANCES_A_RISQUE.get(t);
            if (risque == null) {
                risque = SUBSTANCES_A_RISQUE.get(substanceScannee);
            }
            NiveauGravite gravite = risque != null ? NiveauGravite.ELEVE : NiveauGravite.MOYEN;
            alertes.add(new AlerteDetectee(TypeAlerte.INTERACTION, gravite,
                    "Interaction entre " + m.dci() + " (" + m.nomCommercial() + ") et " + traitement
                            + " (traitement en cours) : "
                            + (risque != null ? risque : "demandez l'avis de votre médecin ou de votre pharmacien") + "."));
        }
        return alertes;
    }

    // ==================== 3. Contre-indication ====================

    private static List<AlerteDetectee> regleContreIndication(MedicamentAnalyse m, ProfilPatient p) {
        List<String> segments = ListeTexte.decouper(m.contreIndications()).stream()
                // les allergies sont traitées par la règle 1.
                .filter(s -> !cle(s).startsWith("allergie") && !cle(s).startsWith("hypersensibilite"))
                .toList();
        List<AlerteDetectee> alertes = new ArrayList<>();

        for (String pathologie : ListeTexte.decouper(p.pathologiesChroniques())) {
            // Mots significatifs : on retire les mots trop généraux AVANT de mettre au singulier
            Set<String> motsCles = mots(cle(pathologie)).stream()
                    .filter(mot -> mot.length() >= 4 && !MOTS_VIDES.contains(mot))
                    .map(InteractionRulesEngine::singulier)
                    .collect(Collectors.toSet());
            if (motsCles.isEmpty()) {
                continue;
            }
            for (String segment : segments) {
                String s = cle(segment);
                if (motsCles.stream().allMatch(s::contains)) {
                    NiveauGravite gravite = s.contains("grossesse") ? NiveauGravite.ELEVE : NiveauGravite.MOYEN;
                    alertes.add(new AlerteDetectee(TypeAlerte.CONTRE_INDICATION, gravite,
                            m.nomCommercial() + " présente une contre-indication (« " + segment
                                    + " ») qui concerne votre pathologie déclarée : " + pathologie
                                    + ". Demandez l'avis de votre médecin avant de le prendre."));
                    break; // une alerte par pathologie
                }
            }
        }
        return alertes;
    }

    // ==================== 4. Expiration ====================

    private static Optional<AlerteDetectee> regleExpiration(MedicamentAnalyse m, LocalDate expiration,
                                                            LocalDate aujourdhui) {
        if (expiration == null) {
            return Optional.empty();
        }
        String nom = m != null ? m.nomCommercial() : "Ce médicament";
        if (expiration.isBefore(aujourdhui)) {
            return Optional.of(new AlerteDetectee(TypeAlerte.EXPIRATION, NiveauGravite.ELEVE,
                    nom + " est périmé depuis " + expiration.format(MOIS_ANNEE)
                            + " : ne l'utilisez pas et rapportez-le en pharmacie."));
        }
        if (!expiration.isAfter(aujourdhui.plusMonths(3))) {
            return Optional.of(new AlerteDetectee(TypeAlerte.EXPIRATION, NiveauGravite.FAIBLE,
                    nom + " expire bientôt (" + expiration.format(JOUR) + ")."));
        }
        return Optional.empty();
    }

    // ==================== Outils de comparaison ====================

    /** Minuscules, sans accents, espaces normalisés. */
    static String cle(String texte) {
        if (texte == null) {
            return "";
        }
        return Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    /** Liste "a, b, c" → ensemble de clés normalisées. */
    private static Set<String> cles(String liste) {
        return ListeTexte.decouper(liste).stream().map(InteractionRulesEngine::cle)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static List<String> mots(String texte) {
        return Arrays.stream(texte.split("[^a-z0-9-]+")).filter(m -> !m.isEmpty()).toList();
    }

    /** "Penicillines" → "penicilline" (les mots courts comme "ains" sont conservés). */
    private static String singulier(String mot) {
        return mot.length() > 4 && mot.endsWith("s") ? mot.substring(0, mot.length() - 1) : mot;
    }
}