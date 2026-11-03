import { Injectable } from '@angular/core';
import { NativeDateAdapter } from '@angular/material/core';

/**
 * Adaptateur de dates Material qui comprend la saisie française "JJ/MM/AAAA".
 * (L'adaptateur natif ne sait lire que le format américain quand on tape la date au clavier.)
 */
@Injectable()
export class DateAdapterFr extends NativeDateAdapter {
  override parse(valeur: unknown, formatLecture?: unknown): Date | null {
    if (typeof valeur === 'string') {
      const morceaux = valeur.trim().match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
      if (morceaux) {
        const jour = Number(morceaux[1]);
        const mois = Number(morceaux[2]);
        const annee = Number(morceaux[3]);
        const date = new Date(annee, mois - 1, jour);
        // Refuse les dates impossibles comme le 31/02/2000
        const valide = date.getFullYear() === annee && date.getMonth() === mois - 1 && date.getDate() === jour;
        return valide ? date : this.invalid();
      }
    }
    return super.parse(valeur, formatLecture);
  }
}