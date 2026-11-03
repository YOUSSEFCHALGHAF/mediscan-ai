import { HttpErrorResponse } from '@angular/common/http';
import { FormGroup } from '@angular/forms';

import { ApiError } from '../models';

/**
 * Extrait un message lisible d'une erreur HTTP renvoyée par l'API (format ApiError).
 */
export function messageErreurApi(err: unknown, parDefaut = 'Une erreur est survenue. Veuillez réessayer.'): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return 'Le serveur est injoignable. Vérifiez votre connexion ou réessayez plus tard.';
    }
    const api = err.error as Partial<ApiError> | null;
    if (api && typeof api === 'object' && typeof api.message === 'string' && api.message) {
      return api.message;
    }
  }
  return parDefaut;
}

/**
 * Reporte les erreurs de validation du serveur (ApiError.details) sur les champs du formulaire.
 * Ex. { "email": "Format d'email invalide" } => erreur "serveur" sur le contrôle "email".
 * Renvoie true si au moins un champ a été marqué.
 */
export function appliquerErreursChamps(form: FormGroup, err: unknown): boolean {
  if (!(err instanceof HttpErrorResponse)) {
    return false;
  }
  const details = (err.error as Partial<ApiError> | null)?.details;
  if (!details) {
    return false;
  }
  let trouve = false;
  for (const [champ, message] of Object.entries(details)) {
    const controle = form.get(champ);
    if (controle) {
      controle.setErrors({ serveur: message });
      controle.markAsTouched();
      trouve = true;
    }
  }
  return trouve;
}