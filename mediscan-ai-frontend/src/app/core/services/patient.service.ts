import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  Anamnese,
  AnamneseRequest,
  ProfilPatientRequest,
  Rappel,
  Scan,
  Utilisateur,
} from '../models';

/**
 * Appels à l'espace patient de l'API (/api/patient/**).
 * Le token est ajouté automatiquement par l'intercepteur : rien à faire ici.
 */
@Service()
export class PatientService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/patient`;

  // ----- Profil -----
  getProfil(): Observable<Utilisateur> {
    return this.http.get<Utilisateur>(`${this.url}/profil`);
  }

  modifierProfil(requete: ProfilPatientRequest): Observable<Utilisateur> {
    return this.http.put<Utilisateur>(`${this.url}/profil`, requete);
  }

  // ----- Anamnèse -----
  getAnamnese(): Observable<Anamnese> {
    return this.http.get<Anamnese>(`${this.url}/anamnese`);
  }

  enregistrerAnamnese(requete: AnamneseRequest): Observable<Anamnese> {
    return this.http.put<Anamnese>(`${this.url}/anamnese`, requete);
  }

  // ----- Scans -----
  getScans(): Observable<Scan[]> {
    return this.http.get<Scan[]>(`${this.url}/scans`);
  }

  getScan(id: number): Observable<Scan> {
    return this.http.get<Scan>(`${this.url}/scans/${id}`);
  }

  // ----- Rappels (lecture ; création/modification à l'étape 17) -----
  getRappels(): Observable<Rappel[]> {
    return this.http.get<Rappel[]>(`${this.url}/rappels`);
  }
}