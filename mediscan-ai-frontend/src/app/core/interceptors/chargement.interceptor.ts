import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';

import { ChargementService } from '../services/chargement.service';
import { SANS_CHARGEMENT } from './contexte-http';

/**
 * Active la barre de chargement globale pendant chaque requête HTTP.
 * finalize() s'exécute dans TOUS les cas : succès, erreur ou annulation.
 */
export const chargementInterceptor: HttpInterceptorFn = (requete, suivant) => {
  if (requete.context.get(SANS_CHARGEMENT)) {
    return suivant(requete);
  }
  const chargement = inject(ChargementService);
  chargement.debut();
  return suivant(requete).pipe(finalize(() => chargement.fin()));
};