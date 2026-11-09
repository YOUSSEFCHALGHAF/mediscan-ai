import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap } from 'rxjs';

import { Frequence, FREQUENCES, Medicament, MedicamentResume, Rappel, RappelRequest } from '../../../core/models';
import { MedicamentService } from '../../../core/services/medicament.service';
import { PatientService } from '../../../core/services/patient.service';
import { depuisDateIso, heureCourte, versDateIso } from '../../../core/utils/dates';
import { appliquerErreursChamps, messageErreurApi } from '../../../core/utils/erreurs-api';
import { messageChamp } from '../../../shared/validation/messages-erreur';

/** Données de la boîte de dialogue : rappel à modifier, ou null pour une création. */
export interface DonneesRappel {
  rappel: Rappel | null;
}

/** Le champ médicament doit contenir un médicament CHOISI dans la liste (pas un simple texte). */
function medicamentChoisi(controle: AbstractControl): ValidationErrors | null {
  const valeur = controle.value;
  return valeur && typeof valeur === 'object' ? null : { medicamentNonChoisi: true };
}

/** La date de fin ne peut pas précéder la date de début (même règle que RappelRequest.java). */
function periodeValide(groupe: AbstractControl): ValidationErrors | null {
  const debut = groupe.get('dateDebut')?.value as Date | null;
  const fin = groupe.get('dateFin')?.value as Date | null;
  return debut && fin && fin < debut ? { periodeInvalide: true } : null;
}

/** Création ou modification d'un rappel de prise. Renvoie le rappel enregistré (ou undefined si annulé). */
@Component({
  selector: 'app-rappel-dialogue',
  imports: [
    ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatAutocompleteModule,
    MatSelectModule, MatDatepickerModule, MatButtonModule, MatIconModule,
  ],
  templateUrl: './rappel-dialogue.component.html',
  styleUrl: './rappel-dialogue.component.scss',
})
export class RappelDialogueComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly patientService = inject(PatientService);
  private readonly medicamentService = inject(MedicamentService);
  private readonly dialogue = inject<MatDialogRef<RappelDialogueComponent, Rappel>>(MatDialogRef);
  protected readonly donnees = inject<DonneesRappel>(MAT_DIALOG_DATA);

  protected readonly frequences = FREQUENCES;
  protected readonly messageChamp = messageChamp;
  protected readonly enregistrement = signal(false);
  protected readonly erreur = signal<string | null>(null);

  protected readonly form = this.fb.group(
    {
      medicament: this.fb.control<MedicamentResume | string | null>(this.donnees.rappel?.medicament ?? null, [
        Validators.required,
        medicamentChoisi,
      ]),
      libelle: [this.donnees.rappel?.libelle ?? '', [Validators.required, Validators.maxLength(150)]],
      heure: [heureCourte(this.donnees.rappel?.heurePrise) || '08:00', [Validators.required, Validators.pattern(/^([01]\d|2[0-3]):[0-5]\d$/)]],
      frequence: this.fb.control<Frequence>(this.donnees.rappel?.frequence ?? 'QUOTIDIEN'),
      dateDebut: this.fb.control<Date | null>(depuisDateIso(this.donnees.rappel?.dateDebut) ?? new Date(), Validators.required),
      dateFin: this.fb.control<Date | null>(depuisDateIso(this.donnees.rappel?.dateFin)),
    },
    { validators: periodeValide },
  );

  /** Suggestions de médicaments (300 ms après la frappe, 2 caractères minimum). */
  protected readonly suggestions = toSignal(
    this.form.controls.medicament.valueChanges.pipe(
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

  protected readonly afficherMedicament = (m: MedicamentResume | string | null): string =>
    typeof m === 'string' ? m : m ? `${m.nomCommercial} ${m.dosage ?? ''}`.trim() : '';

  /** Choix d'un médicament : propose un libellé si le patient n'en a pas saisi. */
  protected choisirMedicament(evenement: MatAutocompleteSelectedEvent): void {
    const medicament = evenement.option.value as MedicamentResume;
    if (!this.form.controls.libelle.value.trim()) {
      this.form.controls.libelle.setValue(medicament.nomCommercial);
    }
  }

  protected enregistrer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const requete: RappelRequest = {
      medicamentId: (v.medicament as MedicamentResume).id,
      libelle: v.libelle.trim(),
      heurePrise: v.heure,
      frequence: v.frequence,
      dateDebut: versDateIso(v.dateDebut)!,
      dateFin: versDateIso(v.dateFin),
      actif: this.donnees.rappel?.actif ?? true,
    };
    const rappel = this.donnees.rappel;
    this.enregistrement.set(true);
    this.erreur.set(null);
    (rappel ? this.patientService.modifierRappel(rappel.id, requete) : this.patientService.creerRappel(requete)).subscribe({
      next: (enregistre) => this.dialogue.close(enregistre),
      error: (err) => {
        this.enregistrement.set(false);
        if (!appliquerErreursChamps(this.form, err)) {
          this.erreur.set(messageErreurApi(err));
        }
      },
    });
  }
}