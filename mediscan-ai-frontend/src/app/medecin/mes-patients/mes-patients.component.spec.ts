import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { PatientResume } from '../../core/models';
import { MesPatientsComponent } from './mes-patients.component';

const PATIENTS: PatientResume[] = [
  { id: 4, nom: 'Lefèvre', prenom: 'Marc', age: 68, sexe: 'HOMME', groupeSanguin: 'O+', alertesNonTraitees: 2,
    perimetreDonnees: 'ANAMNESE,SCANS', dateConsentement: '2026-09-13T09:30:00' },
  { id: 9, nom: 'Durand', prenom: 'Alice', age: 40, sexe: 'FEMME', groupeSanguin: null, alertesNonTraitees: 0,
    perimetreDonnees: 'ANAMNESE', dateConsentement: '2026-10-01T09:30:00' },
];

describe('MesPatientsComponent', () => {
  it('trie par alertes décroissantes et filtre sans tenir compte des accents', async () => {
    await TestBed.configureTestingModule({
      imports: [MesPatientsComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    const fixture = TestBed.createComponent(MesPatientsComponent);
    fixture.detectChanges();
    TestBed.inject(HttpTestingController).expectOne(`${environment.apiUrl}/medecin/patients`).flush(PATIENTS);
    await fixture.whenStable();

    const composant = fixture.componentInstance as unknown as {
      patientsAffiches: () => PatientResume[];
      recherche: { set: (v: string) => void };
    };
    expect(composant.patientsAffiches().map((p) => p.nom)).toEqual(['Lefèvre', 'Durand']);

    composant.recherche.set('lefevre');
    expect(composant.patientsAffiches().map((p) => p.prenom)).toEqual(['Marc']);
  });
});