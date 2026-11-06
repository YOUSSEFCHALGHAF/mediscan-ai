import { Alerte } from '../models';
import { trierAlertes } from './alertes';

const alerte = (id: number, gravite: Alerte['niveauGravite'], traitee: boolean, date = '2026-11-01T10:00:00'): Alerte => ({
  id, type: 'INTERACTION', niveauGravite: gravite, message: '', dateGeneration: date, traitee, scanId: null, medicamentNom: null,
});

describe('trierAlertes', () => {
  it('place les alertes non traitées d\'abord, puis les plus graves', () => {
    const triees = trierAlertes([
      alerte(1, 'FAIBLE', false), alerte(2, 'ELEVE', true), alerte(3, 'ELEVE', false), alerte(4, 'MOYEN', false),
    ]);
    expect(triees.map((a) => a.id)).toEqual([3, 4, 1, 2]);
  });

  it('à gravité égale, la plus récente en premier', () => {
    const triees = trierAlertes([alerte(1, 'MOYEN', false, '2026-10-01T10:00:00'), alerte(2, 'MOYEN', false, '2026-11-01T10:00:00')]);
    expect(triees.map((a) => a.id)).toEqual([2, 1]);
  });
});