/**
 * Règles et calculs pour les images de médicaments envoyées au serveur.
 */

/** Taille maximale acceptée AVANT compression (photo brute d'un smartphone). */
export const TAILLE_MAX_FICHIER_OCTETS = 20 * 1024 * 1024;

/** Le plus grand côté de l'image envoyée : suffisant pour l'OCR, léger à transférer. */
export const DIMENSION_MAX_PX = 1600;

/** Qualité JPEG après compression (0 à 1). */
export const QUALITE_JPEG = 0.85;

const TYPES_ACCEPTES = ['image/jpeg', 'image/png', 'image/webp', 'image/heic', 'image/heif'];

/** Vérifie le fichier choisi par l'utilisateur. Renvoie un message d'erreur, ou null si tout va bien. */
export function verifierFichierImage(fichier: File): string | null {
  if (!TYPES_ACCEPTES.includes(fichier.type)) {
    return 'Format non pris en charge : choisissez une photo JPEG, PNG ou WebP.';
  }
  if (fichier.size > TAILLE_MAX_FICHIER_OCTETS) {
    return 'Cette photo est trop lourde (20 Mo maximum).';
  }
  return null;
}

/**
 * Nouvelles dimensions en gardant les proportions, le plus grand côté limité à "max".
 * Une image déjà petite n'est jamais agrandie.
 */
export function calculerDimensions(largeur: number, hauteur: number, max = DIMENSION_MAX_PX): { largeur: number; hauteur: number } {
  const plusGrandCote = Math.max(largeur, hauteur);
  if (plusGrandCote <= max) {
    return { largeur, hauteur };
  }
  const ratio = max / plusGrandCote;
  return { largeur: Math.round(largeur * ratio), hauteur: Math.round(hauteur * ratio) };
}

/** 1536000 -> "1,5 Mo" */
export function formaterTaille(octets: number): string {
  if (octets < 1024 * 1024) {
    return `${Math.max(1, Math.round(octets / 1024))} Ko`;
  }
  return `${(octets / (1024 * 1024)).toFixed(1).replace('.', ',')} Mo`;
}