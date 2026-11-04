import { etatExpiration, versDateIso } from './dates';

describe('Dates', () => {
  const aujourdhui = new Date(2026, 10, 4); // 4 novembre 2026

  it('détecte un médicament périmé', () => {
    expect(etatExpiration('2026-10-31', aujourdhui)).toBe('perime');
  });

  it('signale une expiration dans les 3 mois', () => {
    expect(etatExpiration('2027-01-31', aujourdhui)).toBe('bientot');
  });

  it('considère valide une date lointaine', () => {
    expect(etatExpiration('2028-03-31', aujourdhui)).toBe('valide');
  });

  it('renvoie null sans date', () => {
    expect(etatExpiration(null, aujourdhui)).toBeNull();
  });

  it('convertit une date sans décalage de fuseau', () => {
    expect(versDateIso(new Date(1990, 5, 15))).toBe('1990-06-15');
  });
});