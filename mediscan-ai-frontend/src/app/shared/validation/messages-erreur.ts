import { AbstractControl } from '@angular/forms';

/**
 * Traduit les erreurs d'un champ de formulaire en message français.
 * Usage dans un template : <mat-error>{{ messageChamp(form.controls.email) }}</mat-error>
 */
export function messageChamp(controle: AbstractControl | null): string {
  const e = controle?.errors;
  if (!e) {
    return '';
  }
  if (e['serveur']) return e['serveur'];
  if (e['required'] || e['requiredTrue']) return 'Ce champ est obligatoire';
  if (e['email']) return 'Format d\'email invalide';
  if (e['minlength']) return `${e['minlength'].requiredLength} caractères minimum`;
  if (e['maxlength']) return `${e['maxlength'].requiredLength} caractères maximum`;
  if (e['motDePasseFaible']) return '8 à 72 caractères, dont au moins une lettre et un chiffre';
  if (e['differents']) return 'Les deux mots de passe ne correspondent pas';
  if (e['telephone']) return 'Numéro invalide (ex. +33 6 12 34 56 78)';
  if (e['rpps']) return 'Le numéro RPPS contient exactement 11 chiffres';
  if (e['dateFuture']) return 'La date doit être dans le passé';
  if (e['matDatepickerParse']) return 'Date invalide (format JJ/MM/AAAA)';
  return 'Valeur invalide';
}