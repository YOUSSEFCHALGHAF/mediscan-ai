import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { Scan } from '../../../core/models';
import { MessagesService } from '../../../core/services/messages.service';
import { DetailScanComponent } from './detail-scan.component';

const SCAN_A_VERIFIER: Scan = {
  id: 4, dateScan: '2026-10-22T16:30:00', statut: 'A_VERIFIER', scoreConfiance: 34, numeroLot: null,
  dateExpiration: null, texteExtrait: 'CLAM... 1g', imageUrl: 'scans/4.jpg', medicament: null,
};
const CLAMOXYL = { id: 6, nomCommercial: 'Clamoxyl', dci: 'amoxicilline', dosage: '1 g' };

/** Accès aux membres "protected" pour le test. */
interface DetailTeste {
  scan(): Scan | null;
  corriger(): void;
  ajouterAuxTraitements(): void;
  supprimer(): void;
}

describe('DetailScanComponent', () => {
  let fixture: ComponentFixture<DetailScanComponent>;
  let http: HttpTestingController;
  const messages = { succes: vi.fn(), erreur: vi.fn() };
  // La boîte de dialogue (correction ou confirmation) renvoie la valeur choisie par le test
  let reponseDialogue: unknown;
  const dialog = { open: vi.fn(() => ({ afterClosed: () => of(reponseDialogue) })) };
  const URL_SCANS = `${environment.apiUrl}/patient/scans`;
  const composant = () => fixture.componentInstance as unknown as DetailTeste;

  beforeEach(async () => {
    vi.clearAllMocks();
    URL.createObjectURL = vi.fn(() => 'blob:x');
    URL.revokeObjectURL = vi.fn();
    await TestBed.configureTestingModule({
      imports: [DetailScanComponent],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        { provide: MessagesService, useValue: messages },
        { provide: MatDialog, useValue: dialog },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(DetailScanComponent);
    fixture.componentRef.setInput('scanId', '4');
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne(`${URL_SCANS}/4`).flush(SCAN_A_VERIFIER);
    await fixture.whenStable();
    // La photo est demandée une fois le scan affiché
    http.expectOne(`${URL_SCANS}/4/image`).flush(new Blob(['x']));
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('corrige le médicament choisi dans la boîte de dialogue', () => {
    reponseDialogue = CLAMOXYL;
    composant().corriger();

    const requete = http.expectOne(`${URL_SCANS}/4/medicament`);
    expect(requete.request.body).toEqual({ medicamentId: 6 });
    requete.flush({ ...SCAN_A_VERIFIER, statut: 'TERMINE', medicament: CLAMOXYL });

    expect(composant().scan()?.statut).toBe('TERMINE');
    expect(messages.succes).toHaveBeenCalledWith('Scan corrigé : Clamoxyl');
  });

  it('n\'envoie rien si la correction est annulée', () => {
    reponseDialogue = undefined;
    composant().corriger();
    http.expectNone(`${URL_SCANS}/4/medicament`);
  });

  it('informe quand la substance est déjà dans les traitements', () => {
    composant().ajouterAuxTraitements();
    http.expectOne(`${URL_SCANS}/4/traitement`).flush({ substance: 'amoxicilline', dejaPresent: true, anamnese: {} });
    expect(messages.succes).toHaveBeenCalledWith('amoxicilline figure déjà dans vos traitements en cours');
  });

  it('supprime après confirmation puis revient à l\'historique', () => {
    const navigation = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    reponseDialogue = true;
    composant().supprimer();

    http.expectOne((r) => r.method === 'DELETE' && r.url === `${URL_SCANS}/4`).flush(null, { status: 204, statusText: 'No Content' });
    expect(messages.succes).toHaveBeenCalledWith('Scan supprimé');
    expect(navigation).toHaveBeenCalled();
  });

  it('ne supprime rien si l\'utilisateur annule', () => {
    reponseDialogue = false;
    composant().supprimer();
    http.expectNone((r) => r.method === 'DELETE');
  });
});