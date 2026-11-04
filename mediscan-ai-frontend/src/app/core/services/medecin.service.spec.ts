import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { MedecinService } from './medecin.service';

describe('MedecinService', () => {
  let service: MedecinService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(MedecinService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('appelle GET /api/medecin/patients', () => {
    service.getPatients().subscribe((liste) => expect(liste).toHaveLength(1));
    const req = http.expectOne(`${environment.apiUrl}/medecin/patients`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 4, nom: 'Lefèvre', prenom: 'Marc', alertesNonTraitees: 2 }]);
  });

  it('appelle GET /api/medecin/patients/{id}', () => {
    service.getFichePatient(4).subscribe((fiche) => expect(fiche.nom).toBe('Lefèvre'));
    http.expectOne(`${environment.apiUrl}/medecin/patients/4`).flush({ id: 4, nom: 'Lefèvre' });
  });
});