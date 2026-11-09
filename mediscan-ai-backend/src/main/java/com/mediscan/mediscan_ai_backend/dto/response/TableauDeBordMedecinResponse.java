package com.mediscan.mediscan_ai_backend.dto.response;

import com.mediscan.mediscan_ai_backend.util.StatistiquesTableauDeBord;

import java.util.List;

/** GET /api/medecin/tableau-de-bord */
public record TableauDeBordMedecinResponse(
        int periodeJours,
        Indicateurs indicateurs,
        List<StatistiquesTableauDeBord.PointJour> evolutionAlertes,
        List<StatistiquesTableauDeBord.CompteType> repartitionParType,
        List<MedicamentScanne> medicamentsLesPlusScannes,
        List<AlerteATraiterResponse> alertesATraiter
) {
    /** Indicateurs clés (cartes en haut du tableau de bord). */
    public record Indicateurs(int patientsSuivis, long alertesNonTraitees, long alertesElevees,
                              long scansSemaine, long rappelsActifs) {
    }

    /** Un médicament et le nombre de fois où il a été scanné par les patients du médecin. */
    public record MedicamentScanne(Long id, String nomCommercial, String dosage, long nombreScans) {
    }
}