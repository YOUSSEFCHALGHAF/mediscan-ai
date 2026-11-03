import { GroupeSanguin, Sexe } from './enums.model';
import { Anamnese } from './anamnese.model';
import { Scan } from './scan.model';
import { Alerte } from './alerte.model';
import { Rappel } from './rappel.model';

/** Ligne de la liste "Mes patients" du médecin (PatientResumeResponse.java). */
export interface PatientResume {
  id: number;
  nom: string;
  prenom: string;
  age: number | null;
  sexe: Sexe | null;
  groupeSanguin: GroupeSanguin | null;
  alertesNonTraitees: number;
  /** Ex. "ANAMNESE,SCANS,ALERTES" */
  perimetreDonnees: string;
  dateConsentement: string;
}

/**
 * Fiche patient vue par le médecin (FichePatientResponse.java).
 * Les catégories NON partagées par le patient sont ABSENTES (d'où les "?").
 */
export interface FichePatient {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  telephone?: string;
  dateNaissance?: string;
  age?: number;
  sexe?: Sexe;
  groupeSanguin?: GroupeSanguin;
  perimetreDonnees: string;
  dateConsentement: string;
  anamnese?: Anamnese;
  scans?: Scan[];
  alertes?: Alerte[];
  rappels?: Rappel[];
}