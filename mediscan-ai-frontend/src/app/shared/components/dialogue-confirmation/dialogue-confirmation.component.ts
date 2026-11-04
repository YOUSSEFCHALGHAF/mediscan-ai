import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { map, Observable } from 'rxjs';

/** Textes de la boîte de dialogue. */
export interface DonneesConfirmation {
  titre: string;
  message: string;
  libelleConfirmer?: string;
  libelleAnnuler?: string;
}

/** Boîte de dialogue "Oui / Non" réutilisable. Renvoie true si l'utilisateur confirme. */
@Component({
  selector: 'app-dialogue-confirmation',
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ donnees.titre }}</h2>
    <mat-dialog-content>{{ donnees.message }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false" cdkFocusInitial>
        {{ donnees.libelleAnnuler ?? 'Annuler' }}
      </button>
      <button mat-flat-button type="button" [mat-dialog-close]="true">
        {{ donnees.libelleConfirmer ?? 'Confirmer' }}
      </button>
    </mat-dialog-actions>
  `,
})
export class DialogueConfirmationComponent {
  protected readonly donnees = inject<DonneesConfirmation>(MAT_DIALOG_DATA);
}

/** Ouvre la boîte de dialogue et renvoie la réponse (true / false). */
export function demanderConfirmation(dialog: MatDialog, donnees: DonneesConfirmation): Observable<boolean> {
  return dialog
    .open(DialogueConfirmationComponent, { data: donnees, width: '420px', maxWidth: '95vw' })
    .afterClosed()
    .pipe(map((reponse) => reponse === true));
}