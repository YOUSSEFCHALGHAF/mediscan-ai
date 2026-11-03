/** Anamnèse du patient (AnamneseResponse.java). id = null si jamais remplie. */
export interface Anamnese {
  id: number | null;
  antecedents: string | null;
  allergies: string | null;
  pathologiesChroniques: string | null;
  traitementsEnCours: string | null;
  habitudesVie: string | null;
  dateMaj: string | null;
  /** true si au moins un champ est renseigné. */
  complete: boolean;
}

/** PUT /api/patient/anamnese (AnamneseRequest.java). Listes séparées par des virgules. */
export interface AnamneseRequest {
  antecedents?: string | null;
  allergies?: string | null;
  pathologiesChroniques?: string | null;
  traitementsEnCours?: string | null;
  habitudesVie?: string | null;
}