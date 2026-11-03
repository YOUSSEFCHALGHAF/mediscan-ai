import { HttpClient } from '@angular/common/http';
import { computed, inject, Service, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  LoginRequest,
  RegisterMedecinRequest,
  RegisterPatientRequest,
  Role,
  Utilisateur,
} from '../models';

const CLE_TOKEN = 'mediscan.token';
const CLE_EXPIRATION = 'mediscan.expiration';
const CLE_UTILISATEUR = 'mediscan.utilisateur';

/**
 * Gère la session de l'utilisateur :
 * connexion, inscription, déconnexion, stockage du token JWT et utilisateur courant (signal).
 *
 * Stockage : localStorage (la session survit au rafraîchissement de la page).
 * Le token est envoyé dans l'en-tête Authorization (intercepteur), jamais dans un cookie :
 * pas de risque CSRF. Le risque XSS est limité par l'échappement automatique d'Angular.
 */
@Service()
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly url = `${environment.apiUrl}/auth`;

  /** Utilisateur connecté (null si déconnecté). Restauré depuis localStorage au démarrage. */
  private readonly _utilisateur = signal<Utilisateur | null>(this.lireSessionStockee());

  readonly utilisateur = this._utilisateur.asReadonly();
  readonly estConnecte = computed(() => this._utilisateur() !== null);
  readonly role = computed<Role | null>(() => this._utilisateur()?.role ?? null);
  readonly nomComplet = computed(() => {
    const u = this._utilisateur();
    return u ? `${u.prenom} ${u.nom}` : '';
  });

  // ---------------- Appels API ----------------

  connecter(requete: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.url}/login`, requete)
      .pipe(tap((reponse) => this.ouvrirSession(reponse)));
  }

  inscrirePatient(requete: RegisterPatientRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.url}/register/patient`, requete)
      .pipe(tap((reponse) => this.ouvrirSession(reponse)));
  }

  inscrireMedecin(requete: RegisterMedecinRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.url}/register/medecin`, requete)
      .pipe(tap((reponse) => this.ouvrirSession(reponse)));
  }

  /** Recharge l'utilisateur depuis le serveur (GET /api/auth/me). */
  rafraichirUtilisateur(): Observable<Utilisateur> {
    return this.http.get<Utilisateur>(`${this.url}/me`).pipe(tap((u) => this.mettreAJourUtilisateur(u)));
  }

  // ---------------- Session ----------------

  /** Met à jour l'utilisateur affiché (ex. après modification du profil). */
  mettreAJourUtilisateur(utilisateur: Utilisateur): void {
    localStorage.setItem(CLE_UTILISATEUR, JSON.stringify(utilisateur));
    this._utilisateur.set(utilisateur);
  }

  /** Token valide ou null (s'il est absent ou expiré, la session est fermée). */
  getToken(): string | null {
    const token = localStorage.getItem(CLE_TOKEN);
    const expiration = Number(localStorage.getItem(CLE_EXPIRATION));
    if (!token || !expiration || Date.now() >= expiration) {
      if (token || this._utilisateur()) {
        this.fermerSession();
      }
      return null;
    }
    return token;
  }

  /** Vrai si un utilisateur est connecté ET que son token n'a pas expiré. */
  sessionValide(): boolean {
    return this.getToken() !== null && this.estConnecte();
  }

  /** Déconnexion volontaire ou forcée (token expiré). */
  deconnecter(raison?: 'expiree'): void {
    this.fermerSession();
    this.router.navigate(['/connexion'], { queryParams: raison ? { raison } : {} });
  }

  /** Page d'accueil de l'espace correspondant au rôle. */
  cheminEspace(role: Role | null = this.role()): string {
    return role === 'MEDECIN' ? '/medecin' : '/patient';
  }

  // ---------------- Interne ----------------

  private ouvrirSession(reponse: AuthResponse): void {
    localStorage.setItem(CLE_TOKEN, reponse.token);
    localStorage.setItem(CLE_EXPIRATION, String(Date.now() + reponse.expiresIn));
    localStorage.setItem(CLE_UTILISATEUR, JSON.stringify(reponse.utilisateur));
    this._utilisateur.set(reponse.utilisateur);
  }

  private fermerSession(): void {
    localStorage.removeItem(CLE_TOKEN);
    localStorage.removeItem(CLE_EXPIRATION);
    localStorage.removeItem(CLE_UTILISATEUR);
    this._utilisateur.set(null);
  }

  private lireSessionStockee(): Utilisateur | null {
    try {
      const expiration = Number(localStorage.getItem(CLE_EXPIRATION));
      const texte = localStorage.getItem(CLE_UTILISATEUR);
      if (!texte || !expiration || Date.now() >= expiration) {
        return null;
      }
      return JSON.parse(texte) as Utilisateur;
    } catch {
      return null;
    }
  }
}