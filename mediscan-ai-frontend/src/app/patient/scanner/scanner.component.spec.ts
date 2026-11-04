import { HttpEventType, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { CompressionImageService } from '../../core/services/compression-image.service';
import { MessagesService } from '../../core/services/messages.service';
import { ScannerComponent } from './scanner.component';

/** Accès aux membres "protected" du composant pour le test. */
interface ScannerTeste {
  etape(): string;
  progression(): number;
  erreur(): string | null;
  resultat(): { id: number } | null;
  preparerFichier(f: File): Promise<void>;
  envoyer(): void;
}

describe('ScannerComponent', () => {
  let fixture: ComponentFixture<ScannerComponent>;
  let http: HttpTestingController;
  const messages = { erreur: vi.fn(), succes: vi.fn() };
  // La compression utilise le canvas du navigateur (absent des tests) : on la simule
  const compression = { compresser: vi.fn(async () => new Blob(['jpeg-compresse'], { type: 'image/jpeg' })) };
  const composant = () => fixture.componentInstance as unknown as ScannerTeste;
  const photo = new File(['photo-brute'], 'boite.jpg', { type: 'image/jpeg' });

  beforeEach(async () => {
    vi.clearAllMocks();
    // URL.createObjectURL n'existe pas dans l'environnement de test
    URL.createObjectURL = vi.fn(() => 'blob:apercu');
    URL.revokeObjectURL = vi.fn();
    await TestBed.configureTestingModule({
      imports: [ScannerComponent],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        { provide: CompressionImageService, useValue: compression },
        { provide: MessagesService, useValue: messages },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ScannerComponent);
    fixture.detectChanges();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('refuse un fichier qui n\'est pas une image, sans le compresser', async () => {
    await composant().preparerFichier(new File(['%PDF'], 'ordonnance.pdf', { type: 'application/pdf' }));
    expect(messages.erreur).toHaveBeenCalled();
    expect(compression.compresser).not.toHaveBeenCalled();
    expect(composant().etape()).toBe('choix');
  });

  it('compresse la photo puis affiche l\'aperçu', async () => {
    await composant().preparerFichier(photo);
    expect(compression.compresser).toHaveBeenCalledWith(photo);
    expect(composant().etape()).toBe('apercu');
  });

  it('suit la progression puis affiche le scan créé', async () => {
    await composant().preparerFichier(photo);
    composant().envoyer();
    expect(composant().etape()).toBe('envoi');

    const req = http.expectOne(`${environment.apiUrl}/patient/scans`);
    req.event({ type: HttpEventType.UploadProgress, loaded: 40, total: 100 });
    expect(composant().progression()).toBe(40);

    req.flush({ id: 12, statut: 'EN_COURS', dateScan: '2026-11-04T10:00:00' }, { status: 201, statusText: 'Created' });
    expect(composant().etape()).toBe('termine');
    expect(composant().resultat()?.id).toBe(12);
  });

  it('conserve la photo et affiche l\'erreur si l\'envoi échoue', async () => {
    await composant().preparerFichier(photo);
    composant().envoyer();
    http.expectOne(`${environment.apiUrl}/patient/scans`).flush(
      { status: 400, message: 'Format non supporté : seules les images JPEG et PNG sont acceptées' },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(composant().etape()).toBe('apercu');
    expect(composant().erreur()).toContain('Format non supporté');
  });
});