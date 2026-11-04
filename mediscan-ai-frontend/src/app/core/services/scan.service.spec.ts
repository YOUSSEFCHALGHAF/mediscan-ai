import { HttpEventType, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { ScanService } from './scan.service';

describe('ScanService', () => {
  let service: ScanService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ScanService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envoie l\'image en multipart dans le champ "image" avec suivi de progression', () => {
    const evenements: HttpEventType[] = [];
    service.envoyer(new Blob(['jpeg'], { type: 'image/jpeg' })).subscribe((e) => evenements.push(e.type));

    const req = http.expectOne(`${environment.apiUrl}/patient/scans`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    expect((req.request.body as FormData).get('image')).toBeInstanceOf(Blob);
    expect(req.request.reportUploadProgress).toBe(true);

    req.event({ type: HttpEventType.UploadProgress, loaded: 50, total: 100 });
    req.flush({ id: 7, statut: 'EN_COURS' }, { status: 201, statusText: 'Created' });
    expect(evenements).toContain(HttpEventType.UploadProgress);
    expect(evenements).toContain(HttpEventType.Response);
  });

  it('télécharge l\'image d\'un scan sous forme de Blob', () => {
    service.getImage(7).subscribe((b) => expect(b).toBeInstanceOf(Blob));
    const req = http.expectOne(`${environment.apiUrl}/patient/scans/7/image`);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['x'], { type: 'image/jpeg' }));
  });
});