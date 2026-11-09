import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { environment } from '../../../../environments/environment';
import { Rappel } from '../../../core/models';
import { RappelDialogueComponent } from './rappel-dialogue.component';

const GLUCOPHAGE = { id: 9, nomCommercial: 'Glucophage', dci: 'metformine', dosage: '850 mg' };
const RAPPEL_SOIR: Rappel = {
  id: 3, libelle: 'Glucophage dîner', heurePrise: '20:00:00', frequence: 'QUOTIDIEN',
  dateDebut: '2026-09-15', dateFin: null, actif: true, medicament: GLUCOPHAGE,
};

/** Accès aux membres "protected" pour le test. */
interface DialogueTeste {
  form: {
    controls: Record<string, { setValue(v: unknown): void; value: unknown }>;
    valid: boolean;
    hasError(code: string): boolean;
  };
  enregistrer(): void;
}

describe('RappelDialogueComponent', () => {
  let fixture: ComponentFixture<RappelDialogueComponent>;
  let http: HttpTestingController;
  const dialogue = { close: vi.fn() };
  const composant = () => fixture.componentInstance as unknown as DialogueTeste;

  async function ouvrir(rappel: Rappel | null): Promise<void> {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [RappelDialogueComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideNativeDateAdapter(),
        { provide: MatDialogRef, useValue: dialogue },
        { provide: MAT_DIALOG_DATA, useValue: { rappel } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(RappelDialogueComponent);
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  }

  afterEach(() => http.verify());

  it('crée un rappel avec l\'heure au format HH:mm et les dates au format ISO', async () => {
    await ouvrir(null);
    const c = composant().form.controls;
    c['medicament'].setValue(GLUCOPHAGE);
    c['libelle'].setValue('Glucophage du matin');
    c['heure'].setValue('08:00');
    c['dateDebut'].setValue(new Date(2026, 10, 4));
    composant().enregistrer();

    const requete = http.expectOne(`${environment.apiUrl}/patient/rappels`);
    expect(requete.request.method).toBe('POST');
    expect(requete.request.body).toEqual({
      medicamentId: 9, libelle: 'Glucophage du matin', heurePrise: '08:00', frequence: 'QUOTIDIEN',
      dateDebut: '2026-11-04', dateFin: null, actif: true,
    });
    requete.flush({ ...RAPPEL_SOIR, id: 10 });
    expect(dialogue.close).toHaveBeenCalled();
  });

  it('pré-remplit le formulaire pour une modification et envoie un PUT', async () => {
    await ouvrir(RAPPEL_SOIR);
    expect(composant().form.controls['heure'].value).toBe('20:00');
    composant().enregistrer();
    const requete = http.expectOne(`${environment.apiUrl}/patient/rappels/3`);
    expect(requete.request.method).toBe('PUT');
    requete.flush(RAPPEL_SOIR);
  });

  it('refuse un médicament tapé au clavier mais non choisi, et une période inversée', async () => {
    await ouvrir(null);
    const c = composant().form.controls;
    c['medicament'].setValue('Glucoph');
    c['libelle'].setValue('Test');
    c['dateDebut'].setValue(new Date(2026, 10, 10));
    c['dateFin'].setValue(new Date(2026, 10, 1));
    expect(composant().form.valid).toBe(false);
    expect(composant().form.hasError('periodeInvalide')).toBe(true);
    composant().enregistrer();
    http.expectNone(`${environment.apiUrl}/patient/rappels`);
  });
});