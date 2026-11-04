import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

import { Role } from '../models';
import { AuthService } from '../services/auth.service';
import { authGuard, inviteGuard, roleGuard } from './auth.guard';

/** Faux AuthService : on choisit l'état de la session pour chaque test. */
class AuthServiceFactice {
  connecte = false;
  roleCourant: Role | null = null;
  sessionValide = () => this.connecte;
  role = () => this.roleCourant;
  cheminEspace = () => (this.roleCourant === 'MEDECIN' ? '/medecin' : '/patient');
}

describe('Guards de navigation', () => {
  let auth: AuthServiceFactice;
  let router: Router;
  const route = {} as ActivatedRouteSnapshot;
  const etat = (url: string) => ({ url }) as RouterStateSnapshot;

  /** Exécute un guard dans le contexte d'injection d'Angular. */
  const executer = (guard: typeof authGuard, url = '/patient/profil') =>
    TestBed.runInInjectionContext(() => guard(route, etat(url)));

  beforeEach(() => {
    auth = new AuthServiceFactice();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    router = TestBed.inject(Router);
  });

  it('authGuard : laisse passer un utilisateur connecté', () => {
    auth.connecte = true;
    expect(executer(authGuard)).toBe(true);
  });

  it('authGuard : renvoie vers /connexion avec returnUrl si non connecté', () => {
    const resultat = executer(authGuard, '/patient/profil') as UrlTree;
    expect(router.serializeUrl(resultat)).toBe('/connexion?returnUrl=%2Fpatient%2Fprofil');
  });

  it('roleGuard : accepte le bon rôle', () => {
    auth.roleCourant = 'PATIENT';
    expect(executer(roleGuard('PATIENT'))).toBe(true);
  });

  it('roleGuard : renvoie un médecin vers son propre espace', () => {
    auth.roleCourant = 'MEDECIN';
    const resultat = executer(roleGuard('PATIENT')) as UrlTree;
    expect(router.serializeUrl(resultat)).toBe('/medecin');
  });

  it('inviteGuard : un utilisateur connecté ne revoit pas la page de connexion', () => {
    auth.connecte = true;
    auth.roleCourant = 'PATIENT';
    expect(router.serializeUrl(executer(inviteGuard) as UrlTree)).toBe('/patient');
  });

  it('inviteGuard : un visiteur accède à la page de connexion', () => {
    expect(executer(inviteGuard)).toBe(true);
  });
});