import { NiveauGravite, TypeAlerte } from './enums.model';

/** Une alerte médicamenteuse (AlerteResponse.java). */
export interface Alerte {
  id: number;
  type: TypeAlerte;
  niveauGravite: NiveauGravite;
  message: string;
  dateGeneration: string;
  traitee: boolean;
  scanId: number | null;
  medicamentNom: string | null;
}