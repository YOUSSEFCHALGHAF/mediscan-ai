import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { environment } from '../../../../environments/environment';
import { MedecinPublic, Partage } from '../../../core/models';
import { DonneesPartage, PartageDialogueComponent } from './partage-dialogue.component';

const MANSOUR: MedecinPublic = { id: 1, nom: 'Mansour', prenom: 'Karim', specialite: 'Cardiologie' };
const DUBOIS: MedecinPublic = { id: 2, nom: 'Dubois', prenom: 'Claire', specialite: 'Médecine générale' };

/** Accès aux membres "protected" pour le test. */
interface DialogueTeste {
  recherche: { setValue(v: unknown): void };
  cases: { controls: Record<string, { setValue(v: boolean): void; value: boolean }>; invalid: boolean };
  medecin(): MedecinPublic | null;
  peutValider(): boolean;
  dejaAutorise(m: MedecinPublic): boolean;
  choisir(e: unknown): void;
  valider(): void;
}

describe('PartageDialogueComponent', () => {
  let fixture: ComponentFixture<PartageDialogueComponent>;
  let http: HttpTestingController;
  const dialogue = { close: vi.fn() };
  const composant = () => fixture.componentInstance as unknown as DialogueTeste;

  async function ouvrir(donnees: DonneesPartage): Promise<void> {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [PartageDialogueComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogue },
        { provide: MAT_DIALOG_DATA, useValue: donnees },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(PartageDialogueComponent);
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  }

  afterEach(() => {
    vi.useRealTimers(); // sécurité : jamais de faux minuteurs laissés actifs pour les tests suivants
    http.verify();
  });

  it('aucune case n\'est cochée par défaut : le patient choisit explicitement', async () => {
    await ouvrir({ partage: null, medecinsAutorises: [] });
    expect(Object.values(composant().cases.controls).map((c) => c.value)).toEqual([false, false, false, false]);
    expect(composant().cases.invalid).toBe(true);
  });

  it('cherche un médecin, signale ceux qui ont déjà accès, puis crée le partage', async () => {
    await ouvrir({ partage: null, medecinsAutorises: [1] });
    // Faux minuteurs APRÈS l'initialisation du composant, pour sauter l'attente de 300 ms de la recherche
    vi.useFakeTimers();
    composant().recherche.setValue('dr');
    vi.advanceTimersByTime(300);
    vi.useRealTimers();
    http.expectOne((r) => r.url === `${environment.apiUrl}/patient/medecins` && r.params.get('q') === 'dr').flush([MANSOUR, DUBOIS]);

    expect(composant().dejaAutorise(MANSOUR)).toBe(true);
    expect(composant().dejaAutorise(DUBOIS)).toBe(false);

    composant().choisir({ option: { value: DUBOIS } });
    composant().cases.controls['ANAMNESE'].setValue(true);
    composant().cases.controls['ALERTES'].setValue(true);
    composant().valider();

    const requete = http.expectOne(`${environment.apiUrl}/patient/partages`);
    expect(requete.request.body).toEqual({ medecinId: 2, categories: ['ANAMNESE', 'ALERTES'] });
    requete.flush({ id: 9 });
    expect(dialogue.close).toHaveBeenCalledWith({ id: 9 });
  });

  it('refuse de valider sans aucune catégorie', async () => {
    await ouvrir({ partage: null, medecinsAutorises: [] });
    composant().choisir({ option: { value: DUBOIS } });
    composant().valider();
    http.expectNone(`${environment.apiUrl}/patient/partages`);
  });

  it('en modification : médecin fixé, cases pré-cochées, envoi d\'un PUT', async () => {
    const partage: Partage = {
      id: 5, medecin: MANSOUR, categories: ['ANAMNESE', 'SCANS', 'ALERTES', 'RAPPELS'],
      dateConsentement: '2026-09-13T09:30:00', dateRevocation: null, actif: true,
    };
    await ouvrir({ partage, medecinsAutorises: [1] });
    expect(composant().medecin()).toEqual(MANSOUR);
    composant().cases.controls['RAPPELS'].setValue(false);
    composant().valider();

    const requete = http.expectOne(`${environment.apiUrl}/patient/partages/5`);
    expect(requete.request.method).toBe('PUT');
    expect(requete.request.body).toEqual({ categories: ['ANAMNESE', 'SCANS', 'ALERTES'] });
    requete.flush({ id: 6 });
  });
});