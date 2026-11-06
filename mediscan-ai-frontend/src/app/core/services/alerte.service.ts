import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Service, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { SANS_CHARGEMENT, SANS_ERREUR_GLOBALE } from '../interceptors/contexte-http';
import { Alerte } from '../models';

/**
 * Alertes médicamenteuses (allergies, interactions, contre-indications, expiration).
 * Tient aussi à jour le NOMBRE d'alertes non traitées du patient, affiché dans le menu.
 */
@Service()
export class AlerteService {
  private readonly http = inject(HttpClient);
  private readonly urlPatient = `${environment.apiUrl}/patient`;
  private readonly urlMedecin = `${environment.apiUrl}/medecin`;

  private readonly _nonTraitees = signal(0);
  /** Nombre d'alertes non traitées du patient connecté (pastille rouge du menu). */
  readonly nonTraitees = this._nonTraitees.asReadonly();

  /** Toutes les alertes du patient connecté (met aussi à jour le compteur). */
  getMesAlertes(context?: HttpContext): Observable<Alerte[]> {
    return this.http
      .get<Alerte[]>(`${this.urlPatient}/alertes`, { context })
      .pipe(tap((alertes) => this._nonTraitees.set(alertes.filter((a) => !a.traitee).length)));
  }

  /** Alertes générées pour un scan donné. */
  getAlertesScan(scanId: number): Observable<Alerte[]> {
    return this.http.get<Alerte[]>(`${this.urlPatient}/scans/${scanId}/alertes`);
  }

  /** Met à jour la pastille du menu, en arrière-plan (ni barre de chargement, ni message d'erreur). */
  actualiserCompteur(): void {
    this.getMesAlertes(new HttpContext().set(SANS_CHARGEMENT, true).set(SANS_ERREUR_GLOBALE, true))
      .subscribe({ error: () => undefined });
  }

  /** Médecin : marque une alerte d'un de ses patients comme traitée. */
  marquerTraitee(patientId: number, alerteId: number): Observable<Alerte> {
    return this.http.put<Alerte>(`${this.urlMedecin}/patients/${patientId}/alertes/${alerteId}/traitee`, null);
  }
}