import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { environment } from '../../../../environments/environment';
import { CorrectionMedicamentComponent } from './correction-medicament.component';

const CLAMOXYL = { id: 6, nomCommercial: 'Clamoxyl', dci: 'amoxicilline', dosage: '1 g' };

/** Accès aux membres "protected" pour le test. */
interface CorrectionTestee {
  recherche: { setValue(v: unknown): void };
  selection(): unknown;
  choisir(e: unknown): void;
  confirmer(): void;
}

describe('CorrectionMedicamentComponent', () => {
  let fixture: ComponentFixture<CorrectionMedicamentComponent>;
  let http: HttpTestingController;
  const dialogue = { close: vi.fn() };
  const composant = () => fixture.componentInstance as unknown as CorrectionTestee;

  beforeEach(async () => {
    vi.useFakeTimers();
    await TestBed.configureTestingModule({
      imports: [CorrectionMedicamentComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogue },
        { provide: MAT_DIALOG_DATA, useValue: { scan: { id: 4, texteExtrait: 'CLAM... 1g ... LOT ...' } } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(CorrectionMedicamentComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  it('recherche dans la base après la frappe puis renvoie le médicament confirmé', () => {
    composant().recherche.setValue('clam');
    vi.advanceTimersByTime(300);
    http.expectOne((r) => r.url === `${environment.apiUrl}/medicaments` && r.params.get('q') === 'clam').flush([CLAMOXYL]);

    // Rien n'est renvoyé tant qu'aucun médicament n'est choisi
    composant().confirmer();
    expect(dialogue.close).not.toHaveBeenCalled();

    composant().choisir({ option: { value: CLAMOXYL } });
    composant().confirmer();
    expect(dialogue.close).toHaveBeenCalledWith(CLAMOXYL);
  });

  it('annule le choix si l\'utilisateur tape à nouveau', () => {
    composant().choisir({ option: { value: CLAMOXYL } });
    composant().recherche.setValue('aug');
    expect(composant().selection()).toBeNull();
    vi.advanceTimersByTime(300);
    http.expectOne((r) => r.params.get('q') === 'aug').flush([]);
  });
});