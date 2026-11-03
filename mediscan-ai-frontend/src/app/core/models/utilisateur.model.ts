import { GroupeSanguin, Role, Sexe } from './enums.model';

/**
 * Utilisateur connecté (UtilisateurResponse.java).
 * Les champs patient OU médecin sont absents selon le rôle (@JsonInclude NON_NULL).
 * Les dates arrivent en texte ISO : "1958-11-03", "2026-09-12T08:45:00".
 */
export interface Utilisateur {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role: Role;
  dateCreation?: string;
  // Patient
  dateNaissance?: string;
  sexe?: Sexe;
  groupeSanguin?: GroupeSanguin;
  // Médecin
  numeroRpps?: string;
  specialite?: string;
  // Commun
  telephone?: string;
}

/** Réponse de connexion / inscription (AuthResponse.java). */
export interface AuthResponse {
  token: string;
  type: 'Bearer';
  /** Durée de validité du token en millisecondes. */
  expiresIn: number;
  utilisateur: Utilisateur;
}

/** POST /api/auth/login (LoginRequest.java). */
export interface LoginRequest {
  email: string;
  motDePasse: string;
}

/** POST /api/auth/register/patient (RegisterPatientRequest.java). */
export interface RegisterPatientRequest {
  nom: string;
  prenom: string;
  email: string;
  motDePasse: string;
  dateNaissance?: string | null;
  sexe?: Sexe | null;
  groupeSanguin?: GroupeSanguin | null;
  telephone?: string | null;
}

/** POST /api/auth/register/medecin (RegisterMedecinRequest.java). */
export interface RegisterMedecinRequest {
  nom: string;
  prenom: string;
  email: string;
  motDePasse: string;
  numeroRpps: string;
  specialite: string;
  telephone?: string | null;
}

/** PUT /api/patient/profil (ProfilPatientRequest.java). */
export interface ProfilPatientRequest {
  nom: string;
  prenom: string;
  dateNaissance?: string | null;
  sexe?: Sexe | null;
  groupeSanguin?: GroupeSanguin | null;
  telephone?: string | null;
}