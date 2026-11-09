import { CompteType, PointJour } from '../models';
import { configEvolution, configMedicaments, configRepartition, jourCourt, resumeEvolution, resumeRepartition } from './configurations';

const POINTS: PointJour[] = [
  { jour: '2026-10-17', elevees: 0, moyennes: 0, faibles: 0 },
  { jour: '2026-10-18', elevees: 1, moyennes: 1, faibles: 0 },
  { jour: '2026-10-19', elevees: 0, moyennes: 0, faibles: 1 },
];

describe('Configurations des graphiques', () => {
  it('formate les jours en JJ/MM', () => {
    expect(jourCourt('2026-10-18')).toBe('18/10');
  });

  it('empile les trois gravités jour par jour', () => {
    const config = configEvolution(POINTS);
    expect(config.data.labels).toEqual(['17/10', '18/10', '19/10']);
    expect(config.data.datasets.map((d) => d.label)).toEqual(['Élevée', 'Moyenne', 'Faible']);
    expect(config.data.datasets[0].data).toEqual([0, 1, 0]);
    expect((config.options?.scales?.['y'] as { stacked: boolean }).stacked).toBe(true);
  });

  it('nomme les types en français et affiche les médicaments en barres horizontales', () => {
    const comptes: CompteType[] = [{ type: 'INTERACTION', nombre: 1 }, { type: 'ALLERGIE', nombre: 0 }];
    expect(configRepartition(comptes).data.labels).toEqual(['Interaction', 'Allergie']);
    const meds = configMedicaments([{ id: 3, nomCommercial: 'Advil', dosage: '400 mg', nombreScans: 2 }]);
    expect(meds.data.labels).toEqual(['Advil 400 mg']);
    expect((meds.options as { indexAxis: string }).indexAxis).toBe('y');
  });

  it('décrit chaque graphique en texte pour les lecteurs d\'écran', () => {
    expect(resumeEvolution(POINTS)).toBe('3 alerte(s) sur les 3 derniers jours, dont 1 de gravité élevée. Jour le plus chargé : 18/10 (2).');
    expect(resumeEvolution([POINTS[0]])).toBe('Aucune alerte sur les 1 derniers jours.');
    expect(resumeRepartition([{ type: 'INTERACTION', nombre: 1 }, { type: 'ALLERGIE', nombre: 0 }])).toBe('Répartition : interaction 1.');
  });
});