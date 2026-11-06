package com.mediscan.mediscan_ai_backend.ia;


import com.mediscan.mediscan_ai_backend.entity.enums.NiveauGravite;
import com.mediscan.mediscan_ai_backend.entity.enums.TypeAlerte;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Moteur de règles testé avec les médicaments et patients de la base de démonstration. */
class InteractionRulesEngineTest {

    private static final InteractionRulesEngine.MedicamentAnalyse DOLIPRANE = new InteractionRulesEngine.MedicamentAnalyse("Doliprane", "paracétamol",
            "insuffisance hépatique sévère, allergie au paracétamol", "warfarine");
    private static final InteractionRulesEngine.MedicamentAnalyse ADVIL = new InteractionRulesEngine.MedicamentAnalyse("Advil", "ibuprofène",
            "ulcère gastroduodénal, insuffisance rénale sévère, insuffisance cardiaque sévère, "
                    + "grossesse à partir du 6e mois, asthme déclenché par les AINS",
            "warfarine, acide acétylsalicylique, méthotrexate, lithium");
    private static final InteractionRulesEngine.MedicamentAnalyse CLAMOXYL = new InteractionRulesEngine.MedicamentAnalyse("Clamoxyl", "amoxicilline",
            "allergie aux pénicillines, mononucléose infectieuse", "méthotrexate, allopurinol");
    private static final InteractionRulesEngine.MedicamentAnalyse AUGMENTIN = new InteractionRulesEngine.MedicamentAnalyse("Augmentin", "amoxicilline, acide clavulanique",
            "allergie aux pénicillines", "méthotrexate, allopurinol");
    private static final InteractionRulesEngine.MedicamentAnalyse COUMADINE = new InteractionRulesEngine.MedicamentAnalyse("Coumadine", "warfarine",
            "grossesse, hémorragie en cours", "acide acétylsalicylique, ibuprofène, paracétamol, amoxicilline");
    private static final InteractionRulesEngine.MedicamentAnalyse GLUCOPHAGE = new InteractionRulesEngine.MedicamentAnalyse("Glucophage", "metformine",
            "insuffisance rénale sévère, acidocétose diabétique, insuffisance hépatique", "produits de contraste iodés, alcool");
    private static final InteractionRulesEngine.MedicamentAnalyse SPASFON = new InteractionRulesEngine.MedicamentAnalyse("Spasfon", "phloroglucinol",
            "hypersensibilité au phloroglucinol", null);

    private static final Map<String, Set<String>> INDEX = InteractionRulesEngine.indexer(
            List.of(DOLIPRANE, ADVIL, CLAMOXYL, AUGMENTIN, COUMADINE, GLUCOPHAGE, SPASFON));

    private static final InteractionRulesEngine.ProfilPatient YASMINE = new InteractionRulesEngine.ProfilPatient("amoxicilline, pénicilline", "asthme", "salbutamol");
    private static final InteractionRulesEngine.ProfilPatient MARC = new InteractionRulesEngine.ProfilPatient("aucune",
            "hypertension artérielle, fibrillation auriculaire", "warfarine, amlodipine");
    private static final InteractionRulesEngine.ProfilPatient NADIA = new InteractionRulesEngine.ProfilPatient("aucune", "diabète de type 2", "metformine");
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 4);

    private static List<InteractionRulesEngine.AlerteDetectee> analyser(InteractionRulesEngine.MedicamentAnalyse m, InteractionRulesEngine.ProfilPatient p) {
        return InteractionRulesEngine.analyser(m, p, INDEX, null, AUJOURDHUI);
    }

    @Test
    @DisplayName("Allergie : Yasmine (amoxicilline) scanne du Clamoxyl -> alerte ÉLEVÉE")
    void allergie() {
        List<InteractionRulesEngine.AlerteDetectee> alertes = analyser(CLAMOXYL, YASMINE);

        assertThat(alertes).hasSize(1);
        assertThat(alertes.get(0).type()).isEqualTo(TypeAlerte.ALLERGIE);
        assertThat(alertes.get(0).gravite()).isEqualTo(NiveauGravite.ELEVE);
        assertThat(alertes.get(0).message()).contains("Clamoxyl").contains("amoxicilline");
    }

    @Test
    @DisplayName("Allergie à une FAMILLE : « pénicillines » couvre l'amoxicilline de l'Augmentin")
    void allergieFamille() {
        assertThat(analyser(AUGMENTIN, new InteractionRulesEngine.ProfilPatient("pénicillines", null, null)))
                .extracting(InteractionRulesEngine.AlerteDetectee::type).containsExactly(TypeAlerte.ALLERGIE);
        assertThat(analyser(ADVIL, new InteractionRulesEngine.ProfilPatient("AINS", null, null)))
                .extracting(InteractionRulesEngine.AlerteDetectee::type).containsExactly(TypeAlerte.ALLERGIE);
    }

    @Test
    @DisplayName("Interaction : Marc (warfarine) scanne de l'Advil -> ÉLEVÉE, risque hémorragique")
    void interaction() {
        List<InteractionRulesEngine.AlerteDetectee> alertes = analyser(ADVIL, MARC);

        assertThat(alertes).hasSize(1);
        assertThat(alertes.get(0).type()).isEqualTo(TypeAlerte.INTERACTION);
        assertThat(alertes.get(0).gravite()).isEqualTo(NiveauGravite.ELEVE);
        assertThat(alertes.get(0).message()).contains("warfarine").contains("risque hémorragique");
    }

    @Test
    @DisplayName("Interaction détectée dans le sens inverse (seule la Coumadine cite l'amoxicilline)")
    void interactionSensInverse() {
        assertThat(analyser(CLAMOXYL, new InteractionRulesEngine.ProfilPatient(null, null, "warfarine")))
                .extracting(InteractionRulesEngine.AlerteDetectee::type, InteractionRulesEngine.AlerteDetectee::gravite)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(TypeAlerte.INTERACTION, NiveauGravite.ELEVE));
    }

    @Test
    @DisplayName("Doublon : Nadia prend déjà de la metformine et scanne du Glucophage")
    void doublon() {
        List<InteractionRulesEngine.AlerteDetectee> alertes = analyser(GLUCOPHAGE, NADIA);

        assertThat(alertes).hasSize(1);
        assertThat(alertes.get(0).gravite()).isEqualTo(NiveauGravite.MOYEN);
        assertThat(alertes.get(0).message()).startsWith("Doublon");
    }

    @Test
    @DisplayName("Contre-indication : Yasmine (asthme) scanne de l'Advil -> MOYENNE")
    void contreIndication() {
        assertThat(analyser(ADVIL, YASMINE))
                .extracting(InteractionRulesEngine.AlerteDetectee::type, InteractionRulesEngine.AlerteDetectee::gravite)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(TypeAlerte.CONTRE_INDICATION, NiveauGravite.MOYEN));
    }

    @Test
    @DisplayName("Contre-indication liée à la grossesse : ÉLEVÉE (« en cours » est ignoré)")
    void grossesse() {
        assertThat(analyser(ADVIL, new InteractionRulesEngine.ProfilPatient(null, "grossesse en cours", null)))
                .extracting(InteractionRulesEngine.AlerteDetectee::gravite).containsExactly(NiveauGravite.ELEVE);
    }

    @Test
    @DisplayName("Pas de fausse contre-indication : insuffisance HÉPATIQUE n'est pas cardiaque ni rénale")
    void pasDeFausseContreIndication() {
        assertThat(analyser(ADVIL, new InteractionRulesEngine.ProfilPatient(null, "insuffisance hépatique", null))).isEmpty();
        assertThat(analyser(ADVIL, new InteractionRulesEngine.ProfilPatient(null, "insuffisance cardiaque", null))).hasSize(1);
    }

    @Test
    @DisplayName("Aucune alerte quand rien ne pose problème")
    void aucuneAlerte() {
        assertThat(analyser(DOLIPRANE, YASMINE)).isEmpty();
        assertThat(analyser(SPASFON, MARC)).isEmpty();
        assertThat(InteractionRulesEngine.analyser(ADVIL, null, INDEX, null, AUJOURDHUI)).isEmpty();
    }

    @Test
    @DisplayName("Expiration : périmé = ÉLEVÉE, dans moins de 3 mois = FAIBLE, plus tard = rien")
    void expiration() {
        assertThat(InteractionRulesEngine.analyser(DOLIPRANE, NADIA, INDEX, LocalDate.of(2026, 10, 31), AUJOURDHUI))
                .extracting(InteractionRulesEngine.AlerteDetectee::gravite).containsExactly(NiveauGravite.ELEVE);
        assertThat(InteractionRulesEngine.analyser(DOLIPRANE, NADIA, INDEX, LocalDate.of(2027, 1, 31), AUJOURDHUI))
                .extracting(InteractionRulesEngine.AlerteDetectee::gravite).containsExactly(NiveauGravite.FAIBLE);
        assertThat(InteractionRulesEngine.analyser(DOLIPRANE, NADIA, INDEX, LocalDate.of(2028, 3, 31), AUJOURDHUI))
                .isEmpty();
    }

    @Test
    @DisplayName("Expiration vérifiée même si le médicament n'a pas été reconnu")
    void expirationSansMedicament() {
        assertThat(InteractionRulesEngine.analyser(null, MARC, INDEX, LocalDate.of(2025, 1, 31), AUJOURDHUI))
                .extracting(InteractionRulesEngine.AlerteDetectee::type).containsExactly(TypeAlerte.EXPIRATION);
    }

    @Test
    @DisplayName("Les alertes sont triées de la plus grave à la moins grave")
    void triParGravite() {
        List<InteractionRulesEngine.AlerteDetectee> alertes = InteractionRulesEngine.analyser(ADVIL,
                new InteractionRulesEngine.ProfilPatient(null, "asthme", "warfarine"), INDEX, LocalDate.of(2027, 1, 15), AUJOURDHUI);

        assertThat(alertes).extracting(InteractionRulesEngine.AlerteDetectee::gravite)
                .containsExactly(NiveauGravite.ELEVE, NiveauGravite.MOYEN, NiveauGravite.FAIBLE);
    }
}