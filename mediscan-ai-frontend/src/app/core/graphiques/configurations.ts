import type { ChartConfiguration } from 'chart.js';

import { CompteType, LIBELLES_TYPE_ALERTE, MedicamentScanne, PointJour, TypeAlerte } from '../models';

/** Couleurs des gravités : variables CSS de styles.scss (repli si indisponibles, ex. dans les tests). */
export interface CouleursGravite {
  eleve: string;
  moyen: string;
  faible: string;
}

const COULEURS_PAR_DEFAUT: CouleursGravite = { eleve: '#ba1a1a', moyen: '#b26a00', faible: '#1b7f3b' };

/** Une couleur distincte par type d'alerte (contrastes vérifiés sur fond clair et sombre). */
export const COULEURS_TYPE: Record<TypeAlerte, string> = {
  ALLERGIE: '#ba1a1a',
  INTERACTION: '#b26a00',
  CONTRE_INDICATION: '#6750a4',
  EXPIRATION: '#00658f',
};

const COULEUR_MEDICAMENTS = '#00658f';

/** Lit les couleurs de gravité définies dans styles.scss (--ms-gravite-*). */
export function lireCouleursGravite(element: Element = document.documentElement): CouleursGravite {
  const style = getComputedStyle(element);
  const lire = (nom: string, repli: string) => style.getPropertyValue(nom).trim() || repli;
  return {
    eleve: lire('--ms-gravite-eleve', COULEURS_PAR_DEFAUT.eleve),
    moyen: lire('--ms-gravite-moyen', COULEURS_PAR_DEFAUT.moyen),
    faible: lire('--ms-gravite-faible', COULEURS_PAR_DEFAUT.faible),
  };
}

/** "2026-10-18" -> "18/10" */
export function jourCourt(iso: string): string {
  const [, mois, jour] = iso.split('-');
  return `${jour}/${mois}`;
}

/** Histogramme empilé : alertes par jour et par gravité. */
export function configEvolution(points: PointJour[], couleurs: CouleursGravite = COULEURS_PAR_DEFAUT): ChartConfiguration {
  return {
    type: 'bar',
    data: {
      labels: points.map((p) => jourCourt(p.jour)),
      datasets: [
        { label: 'Élevée', data: points.map((p) => p.elevees), backgroundColor: couleurs.eleve },
        { label: 'Moyenne', data: points.map((p) => p.moyennes), backgroundColor: couleurs.moyen },
        { label: 'Faible', data: points.map((p) => p.faibles), backgroundColor: couleurs.faible },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: { stacked: true, ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 10 } },
        y: { stacked: true, beginAtZero: true, ticks: { precision: 0 } },
      },
      plugins: { legend: { position: 'bottom' } },
    },
  };
}

/** Anneau : répartition des alertes par type. */
export function configRepartition(comptes: CompteType[]): ChartConfiguration {
  return {
    type: 'doughnut',
    data: {
      labels: comptes.map((c) => LIBELLES_TYPE_ALERTE[c.type]),
      datasets: [{ data: comptes.map((c) => c.nombre), backgroundColor: comptes.map((c) => COULEURS_TYPE[c.type]) }],
    },
    options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
  };
}

/** Barres horizontales : médicaments les plus scannés. */
export function configMedicaments(medicaments: MedicamentScanne[]): ChartConfiguration {
  return {
    type: 'bar',
    data: {
      labels: medicaments.map((m) => `${m.nomCommercial} ${m.dosage ?? ''}`.trim()),
      datasets: [{ label: 'Scans', data: medicaments.map((m) => m.nombreScans), backgroundColor: COULEUR_MEDICAMENTS }],
    },
    options: {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      scales: { x: { beginAtZero: true, ticks: { precision: 0 } } },
      plugins: { legend: { display: false } },
    },
  };
}

// ---------- Textes alternatifs (accessibilité : un graphique doit être compréhensible sans le voir) ----------

export function resumeEvolution(points: PointJour[]): string {
  const total = points.reduce((s, p) => s + p.elevees + p.moyennes + p.faibles, 0);
  const elevees = points.reduce((s, p) => s + p.elevees, 0);
  if (total === 0) {
    return `Aucune alerte sur les ${points.length} derniers jours.`;
  }
  const pic = points.reduce((max, p) => (p.elevees + p.moyennes + p.faibles > max.elevees + max.moyennes + max.faibles ? p : max));
  return `${total} alerte(s) sur les ${points.length} derniers jours, dont ${elevees} de gravité élevée. `
    + `Jour le plus chargé : ${jourCourt(pic.jour)} (${pic.elevees + pic.moyennes + pic.faibles}).`;
}

export function resumeRepartition(comptes: CompteType[]): string {
  const presents = comptes.filter((c) => c.nombre > 0);
  return presents.length
    ? `Répartition : ${presents.map((c) => `${LIBELLES_TYPE_ALERTE[c.type].toLowerCase()} ${c.nombre}`).join(', ')}.`
    : 'Aucune alerte sur la période.';
}

export function resumeMedicaments(medicaments: MedicamentScanne[]): string {
  return medicaments.length
    ? `Médicaments les plus scannés : ${medicaments.map((m) => `${m.nomCommercial} (${m.nombreScans})`).join(', ')}.`
    : 'Aucun médicament scanné sur la période.';
}