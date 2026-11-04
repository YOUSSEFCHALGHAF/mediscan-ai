import { habitudesVersTexte, listeVersTexte, texteVersHabitudes, texteVersListe } from './anamnese';

describe('Conversion de l\'anamnèse', () => {
  it('convertit une liste en texte et inversement', () => {
    expect(listeVersTexte(['pénicilline', 'latex'])).toBe('pénicilline, latex');
    expect(listeVersTexte([])).toBeNull();
    expect(texteVersListe('pénicilline, latex')).toEqual(['pénicilline', 'latex']);
    expect(texteVersListe('aucune')).toEqual([]);
  });

  it('fait l\'aller-retour des habitudes de vie sans perte', () => {
    const habitudes = { tabac: 'ANCIEN' as const, alcool: 'OCCASIONNEL' as const, activite: null, remarques: 'Marche le week-end' };
    const texte = habitudesVersTexte(habitudes);

    expect(texte).toBe('Tabac : Ancien fumeur\nAlcool : Occasionnelle\nRemarques : Marche le week-end');
    expect(texteVersHabitudes(texte)).toEqual(habitudes);
  });

  it('conserve un ancien texte libre dans les remarques', () => {
    const h = texteVersHabitudes('Ancien fumeur (arrêt en 2019)');
    expect(h.tabac).toBeNull();
    expect(h.remarques).toBe('Ancien fumeur (arrêt en 2019)');
  });

  it('renvoie null quand rien n\'est renseigné', () => {
    expect(habitudesVersTexte({ tabac: null, alcool: null, activite: null, remarques: '  ' })).toBeNull();
  });
});