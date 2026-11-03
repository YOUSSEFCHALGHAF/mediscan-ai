import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Validateurs personnalisés : mêmes règles que les DTO Java (@Pattern, @Past),
 * pour prévenir l'utilisateur AVANT l'envoi au serveur.
 */

/** 8 à 72 caractères, au moins une lettre et un chiffre (RegisterPatientRequest.motDePasse). */
export function motDePasseValidator(): ValidatorFn {
  const regle = /^(?=.*[A-Za-z])(?=.*\d).{8,72}$/;
  return (c: AbstractControl): ValidationErrors | null =>
    !c.value || regle.test(c.value) ? null : { motDePasseFaible: true };
}

/** Téléphone facultatif : chiffres et espaces, "+" initial autorisé, 8 à 20 caractères. */
export function telephoneValidator(): ValidatorFn {
  const regle = /^\+?[0-9 ]{8,20}$/;
  return (c: AbstractControl): ValidationErrors | null =>
    !c.value || regle.test(c.value) ? null : { telephone: true };
}

/** Numéro RPPS : exactement 11 chiffres (RegisterMedecinRequest.numeroRpps). */
export function rppsValidator(): ValidatorFn {
  return (c: AbstractControl): ValidationErrors | null =>
    !c.value || /^\d{11}$/.test(c.value) ? null : { rpps: true };
}

/** Date strictement dans le passé (@Past côté Java). */
export function dateDansLePasseValidator(): ValidatorFn {
  return (c: AbstractControl): ValidationErrors | null => {
    const valeur = c.value as Date | null;
    if (!valeur) {
      return null;
    }
    const aujourdhui = new Date();
    aujourdhui.setHours(0, 0, 0, 0);
    return valeur < aujourdhui ? null : { dateFuture: true };
  };
}

/**
 * Validateur de GROUPE : le champ "confirmation" doit être identique à "motDePasse".
 * L'erreur est placée sur le champ de confirmation pour l'afficher sous celui-ci.
 */
export function confirmationIdentiqueValidator(champ = 'motDePasse', confirmation = 'confirmation'): ValidatorFn {
  return (groupe: AbstractControl): ValidationErrors | null => {
    const original = groupe.get(champ);
    const copie = groupe.get(confirmation);
    if (!original || !copie) {
      return null;
    }
    const differents = !!copie.value && original.value !== copie.value;
    const erreursActuelles = { ...(copie.errors ?? {}) };
    if (differents) {
      copie.setErrors({ ...erreursActuelles, differents: true });
    } else if (erreursActuelles['differents']) {
      delete erreursActuelles['differents'];
      copie.setErrors(Object.keys(erreursActuelles).length ? erreursActuelles : null);
    }
    return null;
  };
}