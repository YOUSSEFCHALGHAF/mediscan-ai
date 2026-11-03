import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

/**
 * Intercepteur HTTP exécuté pour CHAQUE requête :
 * 1. ajoute "Authorization: Bearer <token>" aux requêtes vers NOTRE API uniquement
 *    (le token n'est jamais envoyé à un autre site) ;
 * 2. si l'API répond 401 alors qu'un token était envoyé (token expiré/invalide),
 *    déconnecte l'utilisateur et le renvoie vers la page de connexion.
 */
export const authInterceptor: HttpInterceptorFn = (requete, suivant) => {
  const auth = inject(AuthService);
  const versNotreApi = requete.url.startsWith(environment.apiUrl);
  const token = versNotreApi ? auth.getToken() : null;

  const requeteFinale = token
    ? requete.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : requete;

  return suivant(requeteFinale).pipe(
    catchError((erreur: HttpErrorResponse) => {
      if (erreur.status === 401 && token) {
        auth.deconnecter('expiree');
      }
      return throwError(() => erreur);
    }),
  );
};