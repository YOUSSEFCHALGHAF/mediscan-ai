import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { SANS_ERREUR_GLOBALE } from '../interceptors/contexte-http';
import { EtatSante } from '../models';

/**
 * Vérifie que l'API Spring Boot et MySQL répondent.
 * GET {apiUrl}/public/health  (route publique, aucun token nécessaire)
 * SANS_ERREUR_GLOBALE : la carte "État du système" explique elle-même l'erreur.
 */
@Service()
export class SanteService {
  private readonly http = inject(HttpClient);

  verifier(): Observable<EtatSante> {
    return this.http.get<EtatSante>(`${environment.apiUrl}/public/health`, {
      context: new HttpContext().set(SANS_ERREUR_GLOBALE, true),
    });
  }
}