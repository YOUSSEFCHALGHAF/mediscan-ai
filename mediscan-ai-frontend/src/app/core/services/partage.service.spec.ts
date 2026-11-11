import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { PartageService } from './partage.service';

describe('PartageService', () => {
  let service: PartageService;
  let http: HttpTestingController;
  const URL = `${environment.apiUrl}/patient`;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(PartageService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('appelle les bons endpoints avec les bons corps', () => {
    service.rechercherMedecins('card').subscribe();
    const recherche = http.expectOne((r) => r.url === `${URL}/medecins`);
    expect(recherche.request.params.get('q')).toBe('card');
    recherche.flush([]);

    service.creer(1, ['ANAMNESE', 'ALERTES']).subscribe();
    const creation = http.expectOne(`${URL}/partages`);
    expect(creation.request.method).toBe('POST');
    expect(creation.request.body).toEqual({ medecinId: 1, categories: ['ANAMNESE', 'ALERTES'] });
    creation.flush({});

    service.modifierPerimetre(2, ['SCANS']).subscribe();
    const modification = http.expectOne(`${URL}/partages/2`);
    expect(modification.request.method).toBe('PUT');
    expect(modification.request.body).toEqual({ categories: ['SCANS'] });
    modification.flush({});

    service.revoquer(2).subscribe();
    const revocation = http.expectOne((r) => r.method === 'DELETE' && r.url === `${URL}/partages/2`);
    revocation.flush(null, { status: 204, statusText: 'No Content' });
  });
});