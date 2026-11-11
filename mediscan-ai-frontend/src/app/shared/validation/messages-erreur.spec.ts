import { FormControl, Validators } from '@angular/forms';

import { messageChamp } from './messages-erreur';

/** Champ portant exactement les erreurs indiquées. */
const avecErreurs = (erreurs: Record<string, unknown>) => {
  const controle = new FormControl('');
  controle.setErrors(erreurs);
  return controle;
};

describe('messageChamp', () => {
  it('ne renvoie rien pour un champ valide ou absent', () => {
    expect(messageChamp(new FormControl('ok'))).toBe('');
    expect(messageChamp(null)).toBe('');
  });

  it('traduit les erreurs des validateurs Angular', () => {
    expect(messageChamp(new FormControl('', Validators.required))).toBe('Ce champ est obligatoire');
    expect(messageChamp(new FormControl('pas-un-email', Validators.email))).toBe('Format d\'email invalide');
    expect(messageChamp(new FormControl('ab', Validators.minLength(8)))).toBe('8 caractères minimum');
    expect(messageChamp(new FormControl('abcdef', Validators.maxLength(5)))).toBe('5 caractères maximum');
  });

  it('traduit les erreurs des validateurs MediScan', () => {
    expect(messageChamp(avecErreurs({ motDePasseFaible: true }))).toContain('au moins une lettre et un chiffre');
    expect(messageChamp(avecErreurs({ differents: true }))).toBe('Les deux mots de passe ne correspondent pas');
    expect(messageChamp(avecErreurs({ telephone: true }))).toContain('Numéro invalide');
    expect(messageChamp(avecErreurs({ rpps: true }))).toContain('11 chiffres');
    expect(messageChamp(avecErreurs({ dateFuture: true }))).toBe('La date doit être dans le passé');
    expect(messageChamp(avecErreurs({ matDatepickerParse: {} }))).toContain('JJ/MM/AAAA');
  });

  it('donne la priorité au message du serveur, et un message générique sinon', () => {
    expect(messageChamp(avecErreurs({ serveur: 'Cet email est déjà utilisé', required: true }))).toBe('Cet email est déjà utilisé');
    expect(messageChamp(avecErreurs({ inconnue: true }))).toBe('Valeur invalide');
  });
});