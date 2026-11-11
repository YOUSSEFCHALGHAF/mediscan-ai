import { CategoriePartage } from './enums.model';

/** Médecin tel que le patient le voit dans la recherche (MedecinPublicResponse.java). */
export interface MedecinPublic {
  id: number;
  nom: string;
  prenom: string;
  specialite: string;
}

/** Un consentement de partage, actif ou révoqué (PartageResponse.java). */
export interface Partage {
  id: number;
  medecin: MedecinPublic;
  /** Catégories partagées, dans l'ordre ANAMNESE, SCANS, ALERTES, RAPPELS. */
  categories: CategoriePartage[];
  dateConsentement: string;
  dateRevocation: string | null;
  actif: boolean;
}

/** POST /api/patient/partages */
export interface PartageRequest {
  medecinId: number;
  categories: CategoriePartage[];
}

/** PUT /api/patient/partages/{id} */
export interface PerimetreRequest {
  categories: CategoriePartage[];
}