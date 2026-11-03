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