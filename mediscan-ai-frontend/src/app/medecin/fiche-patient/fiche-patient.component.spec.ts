import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { FichePatient } from '../../core/models';
import { FichePatientComponent } from './fiche-patient.component';

/** Fiche de Yasmine telle que la renvoie le backend au Dr Dubois (pas de rappels partagés). */
const FICHE_YASMINE: FichePatient = {
  id: 3,
  nom: 'Haddad',
  prenom: 'Yasmine',
  email: 'yasmine.haddad@mediscan.test',
  age: 31,
  groupeSanguin: 'A+',
  perimetreDonnees: 'ANAMNESE,SCANS,ALERTES',
  dateConsentement: '2026-09-20T11:00:00',
  anamnese: {
    id: 1, antecedents: null, allergies: 'amoxicilline, pénicilline', pathologiesChroniques: 'asthme',
    traitementsEnCours: 'salbutamol', habitudesVie: null, dateMaj: '2026-09-10T14:30:00', complete: true,
  },
  scans: [],
  alertes: [],
};

describe('FichePatientComponent', () => {
  let fixture: ComponentFixture<FichePatientComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FichePatientComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(FichePatientComponent);
    fixture.componentRef.setInput('patientId', '3');
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('affiche "Accès non autorisé" quand le backend répond 403', async () => {
    fixture.detectChanges();
    http.expectOne(`${environment.apiUrl}/medecin/patients/3`).flush(
      { status: 403, message: 'Ce patient ne vous a pas donné son consentement pour consulter ses données' },
      { status: 403, statusText: 'Forbidden' },
    );
    await fixture.whenStable();

    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texte).toContain('Accès non autorisé');
    expect(texte).toContain('consentement');
  });

  it('affiche les allergies et désactive l\'onglet Rappels non partagé', async () => {
    fixture.detectChanges();
    http.expectOne(`${environment.apiUrl}/medecin/patients/3`).flush(FICHE_YASMINE);
    await fixture.whenStable();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('amoxicilline');
    const onglets = Array.from(html.querySelectorAll('[role="tab"]'));
    const ongletRappels = onglets.find((o) => o.textContent?.includes('Rappels'));
    expect(ongletRappels?.getAttribute('aria-disabled')).toBe('true');
  });
});