import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { Scan } from '../../core/models';
import { HistoriqueComponent } from './historique.component';

const scan = (id: number, statut: Scan['statut'], nom: string | null): Scan => ({
  id, statut, dateScan: '2026-11-04T10:00:00', scoreConfiance: 80, numeroLot: null, dateExpiration: null,
  texteExtrait: null, imageUrl: `scans/${id}.jpg`,
  medicament: nom ? { id: 1, nomCommercial: nom, dci: 'x', dosage: '1 mg' } : null,
});

describe('HistoriqueComponent', () => {
  let fixture: ComponentFixture<HistoriqueComponent>;
  let http: HttpTestingController;
  const html = () => fixture.nativeElement as HTMLElement;

  async function creer(statutUrl?: string): Promise<void> {
    URL.createObjectURL = vi.fn(() => 'blob:x');
    URL.revokeObjectURL = vi.fn();
    await TestBed.configureTestingModule({
      imports: [HistoriqueComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(HistoriqueComponent);
    if (statutUrl) {
      fixture.componentRef.setInput('statut', statutUrl);
    }
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne(`${environment.apiUrl}/patient/scans`).flush([
      scan(1, 'TERMINE', 'Doliprane'), scan(2, 'A_VERIFIER', null), scan(3, 'ECHEC', null), scan(4, 'TERMINE', 'Advil'),
    ]);
    await fixture.whenStable();
    // Les miniatures demandent chacune leur image : on y répond
    http.match((r) => r.url.endsWith('/image')).forEach((r) => r.flush(new Blob(['x'])));
    await fixture.whenStable();
  }

  it('affiche tous les scans avec le nombre par statut', async () => {
    await creer();
    expect(html().querySelectorAll('.carte-scan')).toHaveLength(4);
    expect(html().textContent).toContain('Reconnus (2)');
    expect(html().textContent).toContain('À vérifier (1)');
    expect(html().textContent).toContain('Médicament non identifié');
  });

  it('applique le filtre demandé dans l\'URL', async () => {
    await creer('A_VERIFIER');
    expect(html().querySelectorAll('.carte-scan')).toHaveLength(1);
  });
});