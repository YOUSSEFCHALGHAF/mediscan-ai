package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.dto.response.AlerteATraiterResponse;
import com.mediscan.mediscan_ai_backend.dto.response.TableauDeBordMedecinResponse;
import com.mediscan.mediscan_ai_backend.entity.Alerte;
import com.mediscan.mediscan_ai_backend.entity.PartageDonnees;
import com.mediscan.mediscan_ai_backend.entity.enums.NiveauGravite;
import com.mediscan.mediscan_ai_backend.entity.enums.TypeAlerte;
import com.mediscan.mediscan_ai_backend.repository.AlerteRepository;
import com.mediscan.mediscan_ai_backend.repository.PartageDonneesRepository;
import com.mediscan.mediscan_ai_backend.repository.RappelRepository;
import com.mediscan.mediscan_ai_backend.repository.ScanMedicamentRepository;
import com.mediscan.mediscan_ai_backend.util.StatistiquesTableauDeBord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Statistiques du tableau de bord médecin.
 * RÈGLE : chaque statistique ne porte que sur les patients dont le consentement ACTIF
 * couvre la catégorie concernée (ALERTES, SCANS ou RAPPELS).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableauDeBordMedecinService {

    public static final int PERIODE_JOURS = 30;
    public static final int TAILLE_FILE = 10;
    public static final int NB_MEDICAMENTS = 5;

    private final PartageDonneesRepository partageRepository;
    private final AlerteRepository alerteRepository;
    private final ScanMedicamentRepository scanRepository;
    private final RappelRepository rappelRepository;

    /** Identifiants des patients autorisés, catégorie par catégorie. */
    private record Perimetres(int patientsSuivis, List<Long> alertes, List<Long> scans, List<Long> rappels) {
    }

    /**
     * @param aujourdhui date de référence (paramètre : les tests choisissent la date)
     */
    @Transactional(readOnly = true)
    public TableauDeBordMedecinResponse tableauDeBord(Long medecinId, LocalDate aujourdhui) {
        Perimetres p = perimetres(medecinId);
        LocalDate debut = aujourdhui.minusDays(PERIODE_JOURS - 1);
        LocalDateTime depuis = debut.atStartOfDay();

        // ----- Indicateurs -----
        Map<NiveauGravite, Long> nonTraitees = new EnumMap<>(NiveauGravite.class);
        if (!p.alertes().isEmpty()) {
            alerteRepository.compterNonTraiteesParGravite(p.alertes())
                    .forEach(l -> nonTraitees.put((NiveauGravite) l[0], nombre(l[1])));
        }
        long totalNonTraitees = nonTraitees.values().stream().mapToLong(Long::longValue).sum();
        long scansSemaine = p.scans().isEmpty() ? 0
                : scanRepository.countByPatientIdInAndDateScanGreaterThanEqual(p.scans(), aujourdhui.minusDays(6).atStartOfDay());
        long rappelsActifs = p.rappels().isEmpty() ? 0 : rappelRepository.countByPatientIdInAndActifTrue(p.rappels());

        TableauDeBordMedecinResponse.Indicateurs indicateurs = new TableauDeBordMedecinResponse.Indicateurs(p.patientsSuivis(), totalNonTraitees,
                nonTraitees.getOrDefault(NiveauGravite.ELEVE, 0L), scansSemaine, rappelsActifs);

        // ----- Graphiques -----
        List<StatistiquesTableauDeBord.CompteJour> parJour = p.alertes().isEmpty() ? List.of()
                : alerteRepository.compterParJourEtGravite(p.alertes(), depuis).stream()
                .map(l -> new StatistiquesTableauDeBord.CompteJour(versDate(l[0]), (NiveauGravite) l[1], nombre(l[2])))
                .toList();

        Map<TypeAlerte, Long> parType = new EnumMap<>(TypeAlerte.class);
        if (!p.alertes().isEmpty()) {
            alerteRepository.compterParType(p.alertes(), depuis)
                    .forEach(l -> parType.put((TypeAlerte) l[0], nombre(l[1])));
        }

        List<TableauDeBordMedecinResponse.MedicamentScanne> medicaments = p.scans().isEmpty() ? List.of()
                : scanRepository.medicamentsLesPlusScannes(p.scans(), depuis, PageRequest.of(0, NB_MEDICAMENTS)).stream()
                .map(l -> new TableauDeBordMedecinResponse.MedicamentScanne(((Number) l[0]).longValue(), (String) l[1], (String) l[2], nombre(l[3])))
                .toList();

        log.info("Tableau de bord du médecin {} : {} patient(s), {} alerte(s) à traiter",
                medecinId, p.patientsSuivis(), totalNonTraitees);

        return new TableauDeBordMedecinResponse(PERIODE_JOURS, indicateurs,
                StatistiquesTableauDeBord.evolution(parJour, debut, PERIODE_JOURS),
                StatistiquesTableauDeBord.repartition(parType),
                medicaments,
                file(p.alertes(), TAILLE_FILE));
    }

    /** Toutes les alertes non traitées des patients du médecin (page "Alertes"). */
    @Transactional(readOnly = true)
    public List<AlerteATraiterResponse> alertesATraiter(Long medecinId) {
        return file(perimetres(medecinId).alertes(), Integer.MAX_VALUE);
    }

    // ------------------------------------------------------------------

    private Perimetres perimetres(Long medecinId) {
        List<PartageDonnees> partages = partageRepository.findByMedecinIdAndActifTrue(medecinId);
        return new Perimetres(partages.size(),
                patientsCouverts(partages, PartageDonnees.ALERTES),
                patientsCouverts(partages, PartageDonnees.SCANS),
                patientsCouverts(partages, PartageDonnees.RAPPELS));
    }

    private static List<Long> patientsCouverts(List<PartageDonnees> partages, String categorie) {
        return partages.stream()
                .filter(p -> p.couvre(categorie))
                .map(p -> p.getPatient().getId())
                .distinct()
                .toList();
    }

    /** Fil trié : la plus grave d'abord, puis la plus récente. */
    private List<AlerteATraiterResponse> file(List<Long> patients, int limite) {
        if (patients.isEmpty()) {
            return List.of();
        }
        return alerteRepository.findByTraiteeFalseAndPatientIdIn(patients).stream()
                .sorted(StatistiquesTableauDeBord.parGraviteEtDate(Alerte::getNiveauGravite, Alerte::getDateGeneration))
                .limit(limite)
                .map(AlerteATraiterResponse::from)
                .toList();
    }

    private static long nombre(Object valeur) {
        return ((Number) valeur).longValue();
    }

    /** Extract (date ...) renvoie un LocalDate ; on accepte aussi les types SQL selon le pilote. */
    private static LocalDate versDate(Object valeur) {
        if (valeur instanceof LocalDate date) {
            return date;
        }
        if (valeur instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (valeur instanceof LocalDateTime dateHeure) {
            return dateHeure.toLocalDate();
        }
        throw new IllegalStateException("Type de date inattendu : " + valeur);
    }
}