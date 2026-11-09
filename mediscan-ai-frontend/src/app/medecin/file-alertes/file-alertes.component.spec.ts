import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { AlerteATraiter } from '../../core/models';
import { MessagesService } from '../../core/services/messages.service';
import { FileAlertesComponent } from './file-alertes.component';

const ALERTE_MARC: AlerteATraiter = {
  id: 1, type: 'INTERACTION', niveauGravite: 'ELEVE', message: 'Interaction warfarine', traitee: false,
  dateGeneration: '2026-10-18T19:40:05', scanId: 2, medicamentNom: 'Advil', patientId: 4, patientNom: 'Marc Lefèvre',
};

describe('FileAlertesComponent', () => {
  let fixture: ComponentFixture<FileAlertesComponent>;
  let http: HttpTestingController;
  const messages = { succes: vi.fn(), erreur: vi.fn() };
  const html = () => fixture.nativeElement as HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FileAlertesComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: MessagesService, useValue: messages }],
    }).compileComponents();
    fixture = TestBed.createComponent(FileAlertesComponent);
    fixture.componentRef.setInput('alertes', [ALERTE_MARC]);
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('affiche le patient concerné avec un lien vers sa fiche', () => {
    const lien = html().querySelector<HTMLAnchorElement>('a.patient')!;
    expect(lien.textContent).toContain('Marc Lefèvre');
    expect(lien.getAttribute('href')).toBe('/medecin/patients/4');
  });

  it('marque l\'alerte comme traitée chez le bon patient, puis prévient le parent', () => {
    const traitees: AlerteATraiter[] = [];
    fixture.componentInstance.traitee.subscribe((a) => traitees.push(a));

    html().querySelector<HTMLButtonElement>('button')!.click();
    http.expectOne(`${environment.apiUrl}/medecin/patients/4/alertes/1/traitee`).flush({ ...ALERTE_MARC, traitee: true });

    expect(traitees.map((a) => a.id)).toEqual([1]);
    expect(messages.succes).toHaveBeenCalledWith('Alerte de Marc Lefèvre marquée comme traitée');
  });
});