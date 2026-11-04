import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { Anamnese } from '../../core/models';
import { MessagesService } from '../../core/services/messages.service';
import { AnamneseComponent } from './anamnese.component';

/** Anamnèse de Marc telle que renvoyée par le backend (données de démonstration). */
const ANAMNESE_MARC: Anamnese = {
  id: 2,
  antecedents: 'Infarctus du myocarde (2019), pose de stent',
  allergies: 'aucune',
  pathologiesChroniques: 'hypertension artérielle, fibrillation auriculaire',
  traitementsEnCours: 'warfarine, amlodipine',
  habitudesVie: 'Ancien fumeur (arrêt en 2019)',
  dateMaj: '2026-09-12T09:00:00',
  complete: true,
};

describe('AnamneseComponent', () => {
  let fixture: ComponentFixture<AnamneseComponent>;
  let http: HttpTestingController;
  const URL = `${environment.apiUrl}/patient/anamnese`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnamneseComponent],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        { provide: MessagesService, useValue: { succes: vi.fn(), erreur: vi.fn() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(AnamneseComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne(URL).flush(ANAMNESE_MARC);
    await fixture.whenStable(); // affiche les 6 étapes : une erreur de liaison de formulaire ferait échouer le test
  });

  afterEach(() => http.verify());

  /** Accès aux membres "protected" du composant pour les besoins du test. */
  interface ComposantTeste {
    form: {
      getRawValue(): { allergies: string[]; traitements: string[]; habitudes: { remarques: string } };
      controls: { allergies: { setValue(v: string[]): void; markAsDirty(): void } };
    };
    enregistrer(): void;
    aDesModificationsNonEnregistrees(): boolean;
  }
  const composant = () => fixture.componentInstance as unknown as ComposantTeste;

  it('convertit le texte reçu en listes (et ignore "aucune")', () => {
    const v = composant().form.getRawValue();
    expect(v.allergies).toEqual([]);
    expect(v.traitements).toEqual(['warfarine', 'amlodipine']);
    expect(v.habitudes.remarques).toBe('Ancien fumeur (arrêt en 2019)');
    expect(composant().aDesModificationsNonEnregistrees()).toBe(false);
  });

  it('envoie les listes au format texte attendu par l\'API', async () => {
    composant().form.controls.allergies.setValue(['pénicilline', 'latex']);
    composant().form.controls.allergies.markAsDirty();
    expect(composant().aDesModificationsNonEnregistrees()).toBe(true);

    composant().enregistrer();
    const requete = http.expectOne(URL);
    expect(requete.request.method).toBe('PUT');
    expect(requete.request.body.allergies).toBe('pénicilline, latex');
    expect(requete.request.body.traitementsEnCours).toBe('warfarine, amlodipine');
    requete.flush({ ...ANAMNESE_MARC, allergies: 'pénicilline, latex' });

    expect(composant().aDesModificationsNonEnregistrees()).toBe(false);
  });
});