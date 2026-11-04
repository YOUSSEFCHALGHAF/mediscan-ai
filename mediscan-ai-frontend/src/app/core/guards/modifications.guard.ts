import { inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { CanDeactivateFn } from '@angular/router';

import { demanderConfirmation } from '../../shared/components/dialogue-confirmation/dialogue-confirmation.component';

/** Un composant protégé par ce guard doit indiquer s'il a des modifications non enregistrées. */
export interface AvecModificationsNonEnregistrees {
  aDesModificationsNonEnregistrees(): boolean;
}

/**
 * Empêche de quitter une page par erreur (menu, bouton retour...) quand un formulaire
 * contient des modifications non enregistrées : affiche "Quitter sans enregistrer ?".
 */
export const modificationsNonEnregistreesGuard: CanDeactivateFn<AvecModificationsNonEnregistrees> = (composant) => {
  if (!composant.aDesModificationsNonEnregistrees()) {
    return true;
  }
  return demanderConfirmation(inject(MatDialog), {
    titre: 'Quitter sans enregistrer ?',
    message: 'Vos modifications n\'ont pas été enregistrées. Si vous quittez cette page, elles seront perdues.',
    libelleConfirmer: 'Quitter sans enregistrer',
    libelleAnnuler: 'Rester sur la page',
  });
};