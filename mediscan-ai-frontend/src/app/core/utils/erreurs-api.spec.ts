import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup } from '@angular/forms';

import { appliquerErreursChamps, messageErreurApi } from './erreurs-api';

/** Erreur HTTP au format ApiError renvoyé par le GlobalExceptionHandler (étape 6). */
const erreur = (status: number, corps: unknown) => new HttpErrorResponse({ status, error: corps });

describe('messageErreurApi', () => {
  it('affiche le message du serveur (format ApiError)', () => {
    expect(messageErreurApi(erreur(409, { status: 409, message: 'Cet email est déjà utilisé' }))).toBe('Cet email est déjà utilisé');
  });

  it('explique un serveur injoignable (status 0)', () => {
    expect(messageErreurApi(erreur(0, null))).toContain('injoignable');
  });

  it('utilise le message par défaut si la réponse n\'est pas au format ApiError', () => {
    expect(messageErreurApi(erreur(502, '<html>Bad Gateway</html>'), 'Échec')).toBe('Échec');
    expect(messageErreurApi(erreur(500, { message: '' }), 'Échec')).toBe('Échec');
    expect(messageErreurApi(new Error('JS'))).toBe('Une erreur est survenue. Veuillez réessayer.');
  });
});

describe('appliquerErreursChamps', () => {
  const formulaire = () => new FormGroup({ email: new FormControl(''), nom: new FormControl('') });

  it('place chaque erreur du serveur sous le bon champ', () => {
    const form = formulaire();
    const applique = appliquerErreursChamps(form, erreur(400, { details: { email: 'Format d\'email invalide' } }));

    expect(applique).toBe(true);
    expect(form.controls.email.errors).toEqual({ serveur: 'Format d\'email invalide' });
    expect(form.controls.email.touched).toBe(true);
    expect(form.controls.nom.errors).toBeNull();
  });

  it('ignore un champ inconnu du formulaire et les erreurs sans détails', () => {
    const form = formulaire();
    expect(appliquerErreursChamps(form, erreur(400, { details: { inconnu: 'x' } }))).toBe(false);
    expect(appliquerErreursChamps(form, erreur(409, { message: 'Conflit' }))).toBe(false);
    expect(appliquerErreursChamps(form, new Error('pas HTTP'))).toBe(false);
  });
});