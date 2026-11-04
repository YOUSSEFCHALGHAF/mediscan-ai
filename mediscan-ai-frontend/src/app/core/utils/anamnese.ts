import { decouperListe } from './texte';

/**
 * Conversion entre le formulaire d'anamnèse (listes, choix) et le format stocké
 * par l'API (texte). Les listes sont séparées par des virgules : c'est ce format
 * que le moteur de règles IA découpera pour détecter les interactions (étape 16).
 */

export type Tabac = 'NON' | 'ANCIEN' | 'ACTUEL';
export type Alcool = 'NON' | 'OCCASIONNEL' | 'REGULIER';
export type Activite = 'SEDENTAIRE' | 'MODEREE' | 'REGULIERE';

export interface HabitudesVie {
  tabac: Tabac | null;
  alcool: Alcool | null;
  activite: Activite | null;
  remarques: string;
}

export const LIBELLES_TABAC: Record<Tabac, string> = {
  NON: 'Non-fumeur',
  ANCIEN: 'Ancien fumeur',
  ACTUEL: 'Fumeur actuel',
};

export const LIBELLES_ALCOOL: Record<Alcool, string> = {
  NON: 'Aucune consommation',
  OCCASIONNEL: 'Occasionnelle',
  REGULIER: 'Régulière',
};

export const LIBELLES_ACTIVITE: Record<Activite, string> = {
  SEDENTAIRE: 'Sédentaire',
  MODEREE: 'Modérée',
  REGULIERE: 'Régulière',
};

/** ["pénicilline", "latex"] -> "pénicilline, latex" (null si la liste est vide). */
export function listeVersTexte(liste: string[]): string | null {
  return liste.length ? liste.join(', ') : null;
}

/** "pénicilline, latex" -> ["pénicilline", "latex"] */
export function texteVersListe(texte: string | null | undefined): string[] {
  return decouperListe(texte);
}

/**
 * Habitudes de vie -> texte lisible, une information par ligne :
 * "Tabac : Ancien fumeur\nAlcool : Occasionnelle\nActivité physique : Modérée\nRemarques : ..."
 */
export function habitudesVersTexte(h: HabitudesVie): string | null {
  const lignes: string[] = [];
  if (h.tabac) lignes.push(`Tabac : ${LIBELLES_TABAC[h.tabac]}`);
  if (h.alcool) lignes.push(`Alcool : ${LIBELLES_ALCOOL[h.alcool]}`);
  if (h.activite) lignes.push(`Activité physique : ${LIBELLES_ACTIVITE[h.activite]}`);
  if (h.remarques.trim()) lignes.push(`Remarques : ${h.remarques.trim()}`);
  return lignes.length ? lignes.join('\n') : null;
}

/**
 * Texte -> habitudes de vie (opération inverse).
 * Une ligne non reconnue (ex. texte saisi avant ce formulaire) est conservée dans "remarques".
 */
export function texteVersHabitudes(texte: string | null | undefined): HabitudesVie {
  const resultat: HabitudesVie = { tabac: null, alcool: null, activite: null, remarques: '' };
  const autres: string[] = [];

  for (const ligne of (texte ?? '').split('\n').map((l) => l.trim()).filter(Boolean)) {
    const morceaux = ligne.match(/^([^:]+?)\s*:\s*(.+)$/);
    const cle = morceaux?.[1];
    const valeur = morceaux?.[2] ?? '';
    if (cle === 'Tabac' && retrouver(LIBELLES_TABAC, valeur)) {
      resultat.tabac = retrouver(LIBELLES_TABAC, valeur);
    } else if (cle === 'Alcool' && retrouver(LIBELLES_ALCOOL, valeur)) {
      resultat.alcool = retrouver(LIBELLES_ALCOOL, valeur);
    } else if (cle === 'Activité physique' && retrouver(LIBELLES_ACTIVITE, valeur)) {
      resultat.activite = retrouver(LIBELLES_ACTIVITE, valeur);
    } else if (cle === 'Remarques') {
      autres.push(valeur);
    } else {
      autres.push(ligne);
    }
  }
  resultat.remarques = autres.join('\n');
  return resultat;
}

/** Retrouve la clé d'un libellé : "Ancien fumeur" -> "ANCIEN". */
function retrouver<T extends string>(libelles: Record<T, string>, libelle: string): T | null {
  const trouve = (Object.keys(libelles) as T[]).find((cle) => libelles[cle] === libelle);
  return trouve ?? null;
}