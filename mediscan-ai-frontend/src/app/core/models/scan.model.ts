import { Anamnese } from './anamnese.model';
import { StatutScan } from './enums.model';
import { MedicamentResume } from './medicament.model';

/** Un scan de médicament (ScanResponse.java). medicament = null si non reconnu. */
export interface Scan {
  id: number;
  dateScan: string;
  statut: StatutScan;
  scoreConfiance: number | null;
  numeroLot: string | null;
  dateExpiration: string | null;
  texteExtrait: string | null;
  imageUrl: string;
  medicament: MedicamentResume | null;
}

/** PUT /api/patient/scans/{id}/medicament (CorrectionScanRequest.java). */
export interface CorrectionScanRequest {
  medicamentId: number;
}

/** POST /api/patient/scans/{id}/traitement (AjoutTraitementResponse.java). */
export interface AjoutTraitement {
  /** Substance(s) active(s) ajoutée(s) : "ibuprofène". */
  substance: string;
  /** Vrai si la substance figurait déjà dans les traitements en cours. */
  dejaPresent: boolean;
  anamnese: Anamnese;
}