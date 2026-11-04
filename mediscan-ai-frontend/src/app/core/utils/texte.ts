import { CategoriePartage } from '../models';

/** Minuscules + suppression des accents : "Lefèvre" -> "lefevre" (recherche tolérante). */
export function normaliser(texte: string | null | undefined): string {
  return (texte ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();
}

/**
 * Découpe une liste saisie avec des virgules : "amoxicilline, pénicilline" -> ["amoxicilline", "pénicilline"].
 * Les valeurs "aucun / aucune" sont ignorées.
 */
export function decouperListe(texte: string | null | undefined): string[] {
  return (texte ?? '')
    .split(',')
    .map((element) => element.trim())
    .filter((element) => element.length > 0 && !['aucun', 'aucune', 'rien'].includes(normaliser(element)));
}

/** "ANAMNESE,SCANS" -> ["ANAMNESE", "SCANS"] */
export function categoriesPartagees(perimetre: string | null | undefined): CategoriePartage[] {
  return (perimetre ?? '')
    .split(',')
    .map((c) => c.trim().toUpperCase())
    .filter((c): c is CategoriePartage => ['ANAMNESE', 'SCANS', 'ALERTES', 'RAPPELS'].includes(c));
}