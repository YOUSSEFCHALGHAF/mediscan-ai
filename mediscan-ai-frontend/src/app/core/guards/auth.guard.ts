import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Role } from '../models';
import { AuthService } from '../services/auth.service';

/**
 * Protège une route : l'utilisateur doit être connecté (token valide).
 * Sinon -> /connexion?returnUrl=<page demandée> pour y revenir après connexion.
 */
export const authGuard: CanActivateFn = (_route, etat) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.sessionValide()
    ? true
    : router.createUrlTree(['/connexion'], { queryParams: { returnUrl: etat.url } });
};

/**
 * Protège une route par rôle. Ex. canActivate: [authGuard, roleGuard('PATIENT')]
 * Un médecin qui tente d'ouvrir /patient est renvoyé vers son propre espace.
 * (La vraie sécurité reste côté Spring Security : ceci améliore l'ergonomie.)
 */
export function roleGuard(roleAttendu: Role): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    return auth.role() === roleAttendu ? true : router.parseUrl(auth.cheminEspace());
  };
}

/** Pages "invité" (connexion, inscription) : un utilisateur déjà connecté va dans son espace. */
export const inviteGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.sessionValide() ? router.parseUrl(auth.cheminEspace()) : true;
};