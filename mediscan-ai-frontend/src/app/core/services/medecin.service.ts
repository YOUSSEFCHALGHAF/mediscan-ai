import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { FichePatient, PatientResume } from '../models';

/**
 * Appels à l'espace médecin de l'API (/api/medecin/**).
 * Le backend ne renvoie QUE les patients ayant donné un consentement actif.
 */
@Service()
export class MedecinService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/medecin`;

  /** Patients qui m'ont donné leur consentement. */
  getPatients(): Observable<PatientResume[]> {
    return this.http.get<PatientResume[]>(`${this.url}/patients`);
  }

  /** Fiche d'un patient (403 si pas de consentement actif). */
  getFichePatient(patientId: number): Observable<FichePatient> {
    return this.http.get<FichePatient>(`${this.url}/patients/${patientId}`);
  }
}