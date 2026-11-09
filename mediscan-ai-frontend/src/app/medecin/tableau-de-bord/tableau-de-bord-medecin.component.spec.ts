import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { ChargeurGraphiques } from '../../core/graphiques/chargeur-graphiques';
import { TableauDeBordMedecin } from '../../core/models';
import { MessagesService } from '../../core/services/messages.service';
import { TableauDeBordMedecinComponent } from './tableau-de-bord-medecin.component';

/** Tableau de bord du Dr Mansour (données de démonstration, au 4 novembre 2026). */
const DONNEES: TableauDeBordMedecin = {
  periodeJours: 30,
  indicateurs: { patientsSuivis: 1, alertesNonTraitees: 2, alertesElevees: 1, scansSemaine: 0, rappelsActifs: 1 },
  evolutionAlertes: [
    { jour: '2026-10-17', elevees: 0, moyennes: 0, faibles: 0 },
    { jour: '2026-10-18', elevees: 1, moyennes: 1, faibles: 0 },
  ],
  repartitionParType: [
    { type: 'INTERACTION', nombre: 1 }, { type: 'CONTRE_INDICATION', nombre: 1 },
    { type: 'ALLERGIE', nombre: 0 }, { type: 'EXPIRATION', nombre: 0 },
  ],
  medicamentsLesPlusScannes: [],
  alertesATraiter: [
    { id: 1, type: 'INTERACTION', niveauGravite: 'ELEVE', message: 'Interaction warfarine', traitee: false,
      dateGeneration: '2026-10-18T19:40:05', scanId: 2, medicamentNom: 'Advil', patientId: 4, patientNom: 'Marc Lefèvre' },
    { id: 2, type: 'CONTRE_INDICATION', niveauGravite: 'MOYEN', message: 'Insuffisance cardiaque', traitee: false,
      dateGeneration: '2026-10-18T19:40:05', scanId: 2, medicamentNom: 'Advil', patientId: 4, patientNom: 'Marc Lefèvre' },
  ],
};

describe('TableauDeBordMedecinComponent', () => {
  let fixture: ComponentFixture<TableauDeBordMedecinComponent>;
  let http: HttpTestingController;
  const chargeur = { creer: vi.fn(async () => ({ mettreAJour: vi.fn(), detruire: vi.fn() })) };
  const html = () => fixture.nativeElement as HTMLElement;
  const valeurs = () => [...html().querySelectorAll('.indicateur .valeur')].map((e) => e.textContent?.trim());

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [TableauDeBordMedecinComponent],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        { provide: ChargeurGraphiques, useValue: chargeur },
        { provide: MessagesService, useValue: { succes: vi.fn(), erreur: vi.fn() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(TableauDeBordMedecinComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne(`${environment.apiUrl}/medecin/tableau-de-bord`).flush(DONNEES);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('affiche les quatre indicateurs, dont les alertes élevées', () => {
    expect(valeurs()).toEqual(['1', '2', '0', '1']);
    expect(html().textContent).toContain('dont 1 élevée(s)');
  });

  it('dessine les graphiques utiles et un message quand il n\'y a pas de données', () => {
    // Évolution + répartition ; pas de graphique médicaments (aucun scan sur la période)
    expect(chargeur.creer).toHaveBeenCalledTimes(2);
    expect(html().textContent).toContain('Aucun médicament scanné sur la période.');
    const canvas = html().querySelector('canvas')!;
    expect(canvas.getAttribute('aria-label')).toContain('2 alerte(s) sur les 2 derniers jours');
  });

  it('met à jour les indicateurs et la file quand une alerte est traitée', async () => {
    html().querySelector<HTMLButtonElement>('.file button')!.click();
    http.expectOne(`${environment.apiUrl}/medecin/patients/4/alertes/1/traitee`).flush({ ...DONNEES.alertesATraiter[0], traitee: true });
    await fixture.whenStable();

    expect(valeurs()).toEqual(['1', '1', '0', '1']);
    expect(html().textContent).not.toContain('dont 1 élevée(s)');
    expect(html().querySelectorAll('.file li.alerte')).toHaveLength(1);
  });
});