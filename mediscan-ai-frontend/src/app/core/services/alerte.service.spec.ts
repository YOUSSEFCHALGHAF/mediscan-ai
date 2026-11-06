import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { AlerteService } from './alerte.service';

describe('AlerteService', () => {
  let service: AlerteService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AlerteService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('compte les alertes non traitées pour la pastille du menu', () => {
    service.actualiserCompteur();
    http.expectOne(`${environment.apiUrl}/patient/alertes`).flush([
      { id: 1, traitee: false }, { id: 2, traitee: true }, { id: 3, traitee: false },
    ]);
    expect(service.nonTraitees()).toBe(2);
  });

  it('ignore silencieusement une erreur de mise à jour du compteur', () => {
    service.actualiserCompteur();
    http.expectOne(`${environment.apiUrl}/patient/alertes`).flush(null, { status: 500, statusText: 'Server Error' });
    expect(service.nonTraitees()).toBe(0);
  });

  it('marque une alerte comme traitée côté médecin', () => {
    service.marquerTraitee(4, 7).subscribe();
    const req = http.expectOne(`${environment.apiUrl}/medecin/patients/4/alertes/7/traitee`);
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 7, traitee: true });
  });
});