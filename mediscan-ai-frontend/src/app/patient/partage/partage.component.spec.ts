import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Partage } from '../../core/models';
import { MessagesService } from '../../core/services/messages.service';
import { PartageComponent } from './partage.component';

const MANSOUR = { id: 1, nom: 'Mansour', prenom: 'Karim', specialite: 'Cardiologie' };
const ACTIF: Partage = {
  id: 2, medecin: MANSOUR, categories: ['ANAMNESE', 'SCANS', 'ALERTES', 'RAPPELS'],
  dateConsentement: '2026-09-13T09:30:00', dateRevocation: null, actif: true,
};
const REVOQUE: Partage = {
  id: 1, medecin: { id: 2, nom: 'Dubois', prenom: 'Claire', specialite: 'Médecine générale' }, categories: ['SCANS'],
  dateConsentement: '2026-09-01T10:00:00', dateRevocation: '2026-10-01T18:00:00', actif: false,
};

describe('PartageComponent', () => {
  let fixture: ComponentFixture<PartageComponent>;
  let http: HttpTestingController;
  const messages = { succes: vi.fn(), erreur: vi.fn() };
  let reponseDialogue: unknown;
  const dialog = { open: vi.fn(() => ({ afterClosed: () => of(reponseDialogue) })) };
  const URL = `${environment.apiUrl}/patient/partages`;
  const html = () => fixture.nativeElement as HTMLElement;

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [PartageComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: MessagesService, useValue: messages },
        { provide: MatDialog, useValue: dialog },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(PartageComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne(URL).flush([ACTIF, REVOQUE]);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('sépare les accès actifs, avec leurs catégories, de l\'historique', () => {
    expect(html().querySelectorAll('.partage')).toHaveLength(1);
    expect(html().textContent).toContain('Médecins ayant accès (1)');
    expect([...html().querySelectorAll('.partage mat-chip')].map((c) => c.textContent?.trim()))
      .toEqual(['Anamnèse', 'Médicaments', 'Alertes', 'Rappels']);
    expect(html().textContent).toContain('Historique des accès retirés (1)');
  });

  it('indique au dialogue les médecins ayant déjà accès', () => {
    reponseDialogue = undefined;
    fixture.componentInstance.ouvrir();
    expect(dialog.open).toHaveBeenCalledWith(expect.anything(), expect.objectContaining({
      data: { partage: null, medecinsAutorises: [1] },
    }));
  });

  it('révoque après confirmation, puis recharge l\'historique', async () => {
    reponseDialogue = true;
    fixture.componentInstance.revoquer(ACTIF);

    http.expectOne((r) => r.method === 'DELETE' && r.url === `${URL}/2`).flush(null, { status: 204, statusText: 'No Content' });
    http.expectOne(URL).flush([{ ...ACTIF, actif: false, dateRevocation: '2026-11-04T10:00:00' }, REVOQUE]);
    await fixture.whenStable();

    expect(messages.succes).toHaveBeenCalledWith('Accès du Dr Karim Mansour retiré');
    expect(html().textContent).toContain('Médecins ayant accès (0)');
    expect(html().textContent).toContain('Historique des accès retirés (2)');
  });

  it('ne révoque rien si le patient annule', () => {
    reponseDialogue = false;
    fixture.componentInstance.revoquer(ACTIF);
    http.expectNone((r) => r.method === 'DELETE');
  });
});