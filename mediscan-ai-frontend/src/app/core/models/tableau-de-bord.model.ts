import { Alerte } from './alerte.model';
import { TypeAlerte } from './enums.model';

/** Indicateurs clés du médecin (TableauDeBordMedecinResponse.Indicateurs). */
export interface IndicateursMedecin {
  patientsSuivis: number;
  alertesNonTraitees: number;
  alertesElevees: number;
  scansSemaine: number;
  rappelsActifs: number;
}

/** Alertes d'un jour, par gravité. Date au format "AAAA-MM-JJ". */
export interface PointJour {
  jour: string;
  elevees: number;
  moyennes: number;
  faibles: number;
}

export interface CompteType {
  type: TypeAlerte;
  nombre: number;
}

export interface MedicamentScanne {
  id: number;
  nomCommercial: string;
  dosage: string | null;
  nombreScans: number;
}

/** Alerte de la file de traitement : l'alerte + le patient concerné (AlerteATraiterResponse.java). */
export interface AlerteATraiter extends Alerte {
  patientId: number;
  patientNom: string;
}

/** GET /api/medecin/tableau-de-bord (TableauDeBordMedecinResponse.java). */
export interface TableauDeBordMedecin {
  periodeJours: number;
  indicateurs: IndicateursMedecin;
  evolutionAlertes: PointJour[];
  repartitionParType: CompteType[];
  medicamentsLesPlusScannes: MedicamentScanne[];
  alertesATraiter: AlerteATraiter[];
}