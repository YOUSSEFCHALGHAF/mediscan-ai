import { categoriesPartagees, decouperListe, normaliser } from './texte';

describe('Utilitaires de texte', () => {
  it('normalise les accents et la casse', () => {
    expect(normaliser('  Lefèvre ')).toBe('lefevre');
    expect(normaliser('IBUPROFÈNE')).toBe('ibuprofene');
    expect(normaliser(null)).toBe('');
  });

  it('découpe une liste et ignore "aucune"', () => {
    expect(decouperListe('amoxicilline,  pénicilline ,')).toEqual(['amoxicilline', 'pénicilline']);
    expect(decouperListe('aucune')).toEqual([]);
    expect(decouperListe(null)).toEqual([]);
  });

  it('extrait les catégories partagées valides', () => {
    expect(categoriesPartagees('ANAMNESE,SCANS,ALERTES')).toEqual(['ANAMNESE', 'SCANS', 'ALERTES']);
    expect(categoriesPartagees('anamnese, INCONNU')).toEqual(['ANAMNESE']);
  });
});