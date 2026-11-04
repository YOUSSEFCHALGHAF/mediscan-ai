import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';

import { Medicament, Scan } from '../../../core/models';
import { MedicamentService } from '../../../core/services/medicament.service';

/** Données passées à la boîte de dialogue. */
export interface DonneesCorrection {
  scan: Scan;
}

/**
 * Boîte de dialogue "Quel est ce médicament ?" : le patient recherche le bon médicament
 * dans la base et le confirme. Renvoie le médicament choisi (ou undefined si annulé).
 */
@Component({
  selector: 'app-correction-medicament',
  imports: [
    ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule,
    MatAutocompleteModule, MatButtonModule, MatIconModule,
  ],
  templateUrl: './correction-medicament.component.html',
  styleUrl: './correction-medicament.component.scss',
})
export class CorrectionMedicamentComponent {
  private readonly medicamentService = inject(MedicamentService);
  private readonly dialogue = inject<MatDialogRef<CorrectionMedicamentComponent, Medicament>>(MatDialogRef);
  protected readonly donnees = inject<DonneesCorrection>(MAT_DIALOG_DATA);

  protected readonly recherche = new FormControl<string | Medicament>('', { nonNullable: true });
  protected readonly selection = signal<Medicament | null>(null);

  /** Résultats de recherche (300 ms après la dernière frappe, 2 caractères minimum). */
  protected readonly resultats = toSignal(
    this.recherche.valueChanges.pipe(
      // Taper à nouveau annule le choix précédent
      tap((valeur) => typeof valeur === 'string' && this.selection.set(null)),
      map((valeur) => (typeof valeur === 'string' ? valeur.trim() : '')),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((terme) =>
        terme.length < 2
          ? of([] as Medicament[])
          : this.medicamentService.rechercher(terme).pipe(catchError(() => of([] as Medicament[]))),
      ),
    ),
    { initialValue: [] as Medicament[] },
  );

  /** Texte affiché dans le champ après sélection. */
  protected readonly afficher = (valeur: Medicament | string | null): string =>
    typeof valeur === 'string' ? valeur : valeur ? `${valeur.nomCommercial} ${valeur.dosage ?? ''}`.trim() : '';

  protected choisir(evenement: MatAutocompleteSelectedEvent): void {
    this.selection.set(evenement.option.value as Medicament);
  }

  protected confirmer(): void {
    const medicament = this.selection();
    if (medicament) {
      this.dialogue.close(medicament);
    }
  }
}