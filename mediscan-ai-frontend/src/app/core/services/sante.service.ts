import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { EtatSante } from '../models';

/**
 * Vérifie que l'API Spring Boot et MySQL répondent.
 * GET {apiUrl}/public/health  (route publique, aucun token nécessaire)
 */
@Service()
export class SanteService {
  private readonly http = inject(HttpClient);

  verifier(): Observable<EtatSante> {
    return this.http.get<EtatSante>(`${environment.apiUrl}/public/health`);
  }
}