/**
 * Conversions de dates entre Angular Material (objets Date) et l'API (texte ISO "AAAA-MM-JJ").
 * On n'utilise PAS toISOString() : il convertit en UTC et peut décaler la date d'un jour.
 */

/** Date -> "1990-06-15" (date locale, sans fuseau horaire). */
export function versDateIso(date: Date | null | undefined): string | null {
  if (!date || isNaN(date.getTime())) {
    return null;
  }
  const annee = date.getFullYear();
  const mois = String(date.getMonth() + 1).padStart(2, '0');
  const jour = String(date.getDate()).padStart(2, '0');
  return `${annee}-${mois}-${jour}`;
}

/** "1990-06-15" -> Date locale (minuit). */
export function depuisDateIso(texte: string | null | undefined): Date | null {
  if (!texte) {
    return null;
  }
  const [annee, mois, jour] = texte.substring(0, 10).split('-').map(Number);
  return new Date(annee, mois - 1, jour);
}

/** "20:00:00" -> "20:00" */
export function heureCourte(heure: string | null | undefined): string {
  return heure ? heure.substring(0, 5) : '';
}