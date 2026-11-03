/** Version courte d'un médicament (MedicamentResumeResponse.java). */
export interface MedicamentResume {
  id: number;
  nomCommercial: string;
  dci: string;
  dosage: string | null;
}

/** Fiche complète d'un médicament (MedicamentResponse.java). */
export interface Medicament extends MedicamentResume {
  formeGalenique: string | null;
  posologie: string | null;
  contreIndications: string | null;
  interactions: string | null;
  laboratoire: string | null;
}