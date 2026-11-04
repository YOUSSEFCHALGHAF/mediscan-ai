import { TestBed } from '@angular/core/testing';

import { Medicament, Scan } from '../../../core/models';
import { ResultatScanComponent } from './resultat-scan.component';

const SCAN_RECONNU: Scan = {
  id: 12, dateScan: '2026-11-04T10:00:00', statut: 'TERMINE', scoreConfiance: 91,
  numeroLot: '24B117', dateExpiration: '2020-03-31', texteExtrait: 'DOLIPRANE 1000 mg', imageUrl: 'scans/x.jpg',
  medicament: { id: 1, nomCommercial: 'Doliprane', dci: 'paracétamol', dosage: '1000 mg' },
};

const FICHE: Medicament = {
  id: 1, nomCommercial: 'Doliprane', dci: 'paracétamol', dosage: '1000 mg', formeGalenique: 'comprimé',
  posologie: '1 comprimé toutes les 6 heures', contreIndications: 'insuffisance hépatique sévère, allergie au paracétamol',
  interactions: 'warfarine', laboratoire: 'Sanofi',
};

describe('ResultatScanComponent', () => {
  async function afficher(scan: Scan, medicament: Medicament | null): Promise<HTMLElement> {
    await TestBed.configureTestingModule({ imports: [ResultatScanComponent] }).compileComponents();
    const fixture = TestBed.createComponent(ResultatScanComponent);
    fixture.componentRef.setInput('scan', scan);
    fixture.componentRef.setInput('medicament', medicament);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('affiche la fiche du médicament reconnu et signale une date dépassée', async () => {
    const html = await afficher(SCAN_RECONNU, FICHE);
    expect(html.textContent).toContain('Médicament reconnu');
    expect(html.textContent).toContain('Doliprane');
    expect(html.textContent).toContain('1 comprimé toutes les 6 heures');
    expect(html.querySelectorAll('.rubrique.alerte li')).toHaveLength(2);
    expect(html.textContent).toContain('Périmé');
  });

  it('explique un scan à vérifier', async () => {
    const html = await afficher({ ...SCAN_RECONNU, statut: 'A_VERIFIER', medicament: null, dateExpiration: null }, null);
    expect(html.textContent).toContain('non identifié avec certitude');
    expect(html.textContent).toContain('Reprenez la photo');
  });
});