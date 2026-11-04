import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { SANS_CHARGEMENT, SANS_ERREUR_GLOBALE } from '../interceptors/contexte-http';
import { Medicament } from '../models';

/** Consultation de la base médicamenteuse (/api/medicaments). */
@Service()
export class MedicamentService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/medicaments`;

  /**
   * Recherche par nom commercial ou DCI (2 caractères minimum).
   * Utilisée pendant la frappe (autocomplétion) : pas de barre de chargement
   * ni de notification d'erreur, pour ne pas gêner la saisie.
   */
  rechercher(terme: string): Observable<Medicament[]> {
    return this.http.get<Medicament[]>(this.url, {
      params: { q: terme },
      context: new HttpContext().set(SANS_CHARGEMENT, true).set(SANS_ERREUR_GLOBALE, true),
    });
  }

  getMedicament(id: number): Observable<Medicament> {
    return this.http.get<Medicament>(`${this.url}/${id}`);
  }
}