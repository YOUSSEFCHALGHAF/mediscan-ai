import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Rappel } from '../../core/models';
import { NotificationPushService } from '../../core/notifications/notification-push.service';
import { MessagesService } from '../../core/services/messages.service';
import { RappelsComponent } from './rappels.component';

const GLUCOPHAGE = { id: 9, nomCommercial: 'Glucophage', dci: 'metformine', dosage: '850 mg' };
const rappel = (id: number, heure: string, actif = true): Rappel => ({
  id, libelle: `Rappel ${heure}`, heurePrise: `${heure}:00`, frequence: 'QUOTIDIEN',
  dateDebut: '2026-09-15', dateFin: null, actif, medicament: GLUCOPHAGE,
});

describe('RappelsComponent', () => {
  let fixture: ComponentFixture<RappelsComponent>;
  let http: HttpTestingController;
  const messages = { succes: vi.fn(), erreur: vi.fn() };
  let reponseDialogue: unknown = true;
  const dialog = { open: vi.fn(() => ({ afterClosed: () => of(reponseDialogue) })) };
  const URL_RAPPELS = `${environment.apiUrl}/patient/rappels`;
  const html = () => fixture.nativeElement as HTMLElement;

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [RappelsComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: MessagesService, useValue: messages },
        { provide: MatDialog, useValue: dialog },
        { provide: NotificationPushService, useValue: { etat: signal('a-demander'), activer: vi.fn() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(RappelsComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne(URL_RAPPELS).flush([rappel(1, '20:00'), rappel(2, '08:00')]);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('affiche les rappels triés par heure et invite à activer les notifications', () => {
    const heures = [...html().querySelectorAll('.heure')].map((e) => e.textContent?.trim());
    expect(heures).toEqual(['08:00', '20:00']);
    expect(html().textContent).toContain('Activez les notifications');
  });

  it('met un rappel en pause en conservant ses autres informations', () => {
    fixture.componentInstance.basculer(rappel(1, '20:00'), false);
    const requete = http.expectOne(`${URL_RAPPELS}/1`);
    expect(requete.request.method).toBe('PUT');
    expect(requete.request.body).toEqual({
      medicamentId: 9, libelle: 'Rappel 20:00', heurePrise: '20:00', frequence: 'QUOTIDIEN',
      dateDebut: '2026-09-15', dateFin: null, actif: false,
    });
    requete.flush(rappel(1, '20:00', false));
    expect(messages.succes).toHaveBeenCalledWith('Rappel mis en pause');
  });

  it('supprime un rappel après confirmation', async () => {
    reponseDialogue = true;
    fixture.componentInstance.supprimer(rappel(2, '08:00'));
    http.expectOne((r) => r.method === 'DELETE' && r.url === `${URL_RAPPELS}/2`).flush(null);
    await fixture.whenStable();
    expect(html().querySelectorAll('.rappel')).toHaveLength(1);
  });
});