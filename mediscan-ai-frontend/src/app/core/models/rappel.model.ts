import { Frequence } from './enums.model';
import { MedicamentResume } from './medicament.model';

/** Un rappel de prise (RappelResponse.java). heurePrise au format "HH:mm:ss". */
export interface Rappel {
  id: number;
  libelle: string;
  heurePrise: string;
  frequence: Frequence;
  dateDebut: string;
  dateFin: string | null;
  actif: boolean;
  medicament: MedicamentResume;
}

/** POST / PUT /api/patient/rappels (RappelRequest.java). heurePrise au format "HH:mm". */
export interface RappelRequest {
  medicamentId: number;
  libelle: string;
  heurePrise: string;
  frequence?: Frequence | null;
  dateDebut: string;
  dateFin?: string | null;
  actif?: boolean | null;
}