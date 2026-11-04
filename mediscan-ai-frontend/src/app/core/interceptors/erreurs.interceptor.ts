import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { MessagesService } from '../services/messages.service';
import { ReseauService } from '../services/reseau.service';
import { SANS_ERREUR_GLOBALE } from './contexte-http';

/**
 * Notification GLOBALE des erreurs techniques, sans code répété dans chaque page :
 * - 0   : serveur injoignable (backend arrêté, CORS) — sauf si hors ligne (un bandeau s'en charge) ;
 * - 5xx : erreur interne du serveur.
 * Les erreurs "métier" (400, 401, 403, 404, 409) restent gérées par chaque page,
 * qui affiche un message adapté (sous le champ, carte "Accès non autorisé"...).
 * L'erreur est toujours renvoyée au composant (throwError).
 */
export const erreursInterceptor: HttpInterceptorFn = (requete, suivant) => {
  const messages = inject(MessagesService);
  const reseau = inject(ReseauService);

  return suivant(requete).pipe(
    catchError((erreur: HttpErrorResponse) => {
      if (!requete.context.get(SANS_ERREUR_GLOBALE)) {
        if (erreur.status === 0 && reseau.enLigne()) {
          messages.erreur('Le serveur MediScan est injoignable. Réessayez dans quelques instants.');
        } else if (erreur.status >= 500) {
          messages.erreur('Une erreur est survenue sur le serveur. Veuillez réessayer.');
        }
      }
      return throwError(() => erreur);
    }),
  );
};