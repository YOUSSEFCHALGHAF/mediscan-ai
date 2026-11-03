/**
 * Énumérations partagées avec le backend.
 * Les valeurs sont IDENTIQUES aux enums Java (Role, StatutScan, TypeAlerte, NiveauGravite)
 * et aux contraintes CHECK de MySQL.
 */

export type Role = 'PATIENT' | 'MEDECIN';

export type StatutScan = 'EN_COURS' | 'TERMINE' | 'A_VERIFIER' | 'ECHEC';

export type TypeAlerte = 'INTERACTION' | 'CONTRE_INDICATION' | 'ALLERGIE' | 'EXPIRATION';

export type NiveauGravite = 'FAIBLE' | 'MOYEN' | 'ELEVE';

export type Frequence = 'QUOTIDIEN' | 'HEBDOMADAIRE' | 'PONCTUEL';

export type Sexe = 'HOMME' | 'FEMME' | 'AUTRE';

export type GroupeSanguin = 'A+' | 'A-' | 'B+' | 'B-' | 'AB+' | 'AB-' | 'O+' | 'O-';

/** Catégories de données qu'un patient peut partager avec un médecin. */
export type CategoriePartage = 'ANAMNESE' | 'SCANS' | 'ALERTES' | 'RAPPELS';

/** Option affichable dans une liste déroulante (valeur technique + libellé français). */
export interface Option<T extends string> {
  valeur: T;
  libelle: string;
}

export const SEXES: Option<Sexe>[] = [
  { valeur: 'FEMME', libelle: 'Femme' },
  { valeur: 'HOMME', libelle: 'Homme' },
  { valeur: 'AUTRE', libelle: 'Autre' },
];

export const GROUPES_SANGUINS: GroupeSanguin[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

export const FREQUENCES: Option<Frequence>[] = [
  { valeur: 'QUOTIDIEN', libelle: 'Tous les jours' },
  { valeur: 'HEBDOMADAIRE', libelle: 'Une fois par semaine' },
  { valeur: 'PONCTUEL', libelle: 'Ponctuel' },
];

export const CATEGORIES_PARTAGE: Option<CategoriePartage>[] = [
  { valeur: 'ANAMNESE', libelle: 'Anamnèse (antécédents, allergies, traitements)' },
  { valeur: 'SCANS', libelle: 'Médicaments scannés' },
  { valeur: 'ALERTES', libelle: 'Alertes médicamenteuses' },
  { valeur: 'RAPPELS', libelle: 'Rappels de prise' },
];

export const LIBELLES_STATUT_SCAN: Record<StatutScan, string> = {
  EN_COURS: 'Analyse en cours',
  TERMINE: 'Reconnu',
  A_VERIFIER: 'À vérifier',
  ECHEC: 'Échec',
};

export const LIBELLES_TYPE_ALERTE: Record<TypeAlerte, string> = {
  INTERACTION: 'Interaction',
  CONTRE_INDICATION: 'Contre-indication',
  ALLERGIE: 'Allergie',
  EXPIRATION: 'Expiration',
};

export const LIBELLES_GRAVITE: Record<NiveauGravite, string> = {
  FAIBLE: 'Faible',
  MOYEN: 'Moyenne',
  ELEVE: 'Élevée',
};