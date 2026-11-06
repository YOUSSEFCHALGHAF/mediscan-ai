import { Alerte, NiveauGravite, TypeAlerte } from '../models';

const ORDRE_GRAVITE: Record<NiveauGravite, number> = { ELEVE: 0, MOYEN: 1, FAIBLE: 2 };

/** Non traitées d'abord, puis de la plus grave à la moins grave, puis de la plus récente à la plus ancienne. */
export function trierAlertes(alertes: Alerte[]): Alerte[] {
  return [...alertes].sort(
    (a, b) =>
      Number(a.traitee) - Number(b.traitee) ||
      ORDRE_GRAVITE[a.niveauGravite] - ORDRE_GRAVITE[b.niveauGravite] ||
      b.dateGeneration.localeCompare(a.dateGeneration),
  );
}

/** Icône Material de chaque type d'alerte. */
export const ICONES_TYPE_ALERTE: Record<TypeAlerte, string> = {
  ALLERGIE: 'allergy',
  INTERACTION: 'sync_problem',
  CONTRE_INDICATION: 'do_not_disturb_on',
  EXPIRATION: 'event_busy',
};