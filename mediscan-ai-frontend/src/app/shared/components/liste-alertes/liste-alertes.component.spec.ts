import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Alerte } from '../../../core/models';
import { ListeAlertesComponent } from './liste-alertes.component';

const ALERTES: Alerte[] = [
  { id: 1, type: 'EXPIRATION', niveauGravite: 'FAIBLE', message: 'Expire bientôt', dateGeneration: '2026-11-01T10:00:00',
    traitee: false, scanId: 9, medicamentNom: 'Doliprane' },
  { id: 2, type: 'INTERACTION', niveauGravite: 'ELEVE', message: 'Interaction warfarine', dateGeneration: '2026-11-01T10:00:00',
    traitee: false, scanId: 9, medicamentNom: 'Advil' },
];

describe('ListeAlertesComponent', () => {
  let fixture: ComponentFixture<ListeAlertesComponent>;
  const html = () => fixture.nativeElement as HTMLElement;

  async function afficher(peutTraiter: boolean): Promise<void> {
    await TestBed.configureTestingModule({ imports: [ListeAlertesComponent], providers: [provideRouter([])] }).compileComponents();
    fixture = TestBed.createComponent(ListeAlertesComponent);
    fixture.componentRef.setInput('alertes', ALERTES);
    fixture.componentRef.setInput('peutTraiter', peutTraiter);
    await fixture.whenStable();
  }

  it('affiche la plus grave en premier, sans bouton pour le patient', async () => {
    await afficher(false);
    const messages = [...html().querySelectorAll('.message')].map((p) => p.textContent);
    expect(messages).toEqual(['Interaction warfarine', 'Expire bientôt']);
    expect(html().querySelector('button')).toBeNull();
  });

  it('émet l\'alerte à traiter quand le médecin clique', async () => {
    await afficher(true);
    const emises: Alerte[] = [];
    fixture.componentInstance.traiter.subscribe((a) => emises.push(a));
    html().querySelector<HTMLButtonElement>('button')!.click();
    expect(emises[0].id).toBe(2);
  });
});