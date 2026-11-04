import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { ImageScanComponent } from './image-scan.component';

describe('ImageScanComponent', () => {
  let fixture: ComponentFixture<ImageScanComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    URL.createObjectURL = vi.fn(() => 'blob:photo-7');
    URL.revokeObjectURL = vi.fn();
    await TestBed.configureTestingModule({
      imports: [ImageScanComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(ImageScanComponent);
    fixture.componentRef.setInput('scanId', 7);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('télécharge l\'image avec le token puis l\'affiche via une URL locale', async () => {
    http.expectOne(`${environment.apiUrl}/patient/scans/7/image`).flush(new Blob(['jpeg'], { type: 'image/jpeg' }));
    await fixture.whenStable();
    const img = (fixture.nativeElement as HTMLElement).querySelector('img');
    expect(img?.getAttribute('src')).toBe('blob:photo-7');
  });

  it('affiche un substitut si la photo est introuvable', async () => {
    http.expectOne(`${environment.apiUrl}/patient/scans/7/image`).flush(
      new Blob(), { status: 404, statusText: 'Not Found' },
    );
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).querySelector('img')).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('image_not_supported');
  });

  it('libère l\'URL locale quand le composant disparaît', async () => {
    http.expectOne(`${environment.apiUrl}/patient/scans/7/image`).flush(new Blob(['jpeg']));
    await fixture.whenStable();
    fixture.destroy();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:photo-7');
  });
});