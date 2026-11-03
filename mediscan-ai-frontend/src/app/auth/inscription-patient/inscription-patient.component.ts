import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';

import { GroupeSanguin, GROUPES_SANGUINS, Sexe, SEXES } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { versDateIso } from '../../core/utils/dates';
import { appliquerErreursChamps, messageErreurApi } from '../../core/utils/erreurs-api';
import { messageChamp } from '../../shared/validation/messages-erreur';
import {
  confirmationIdentiqueValidator,
  dateDansLePasseValidator,
  motDePasseValidator,
  telephoneValidator,
} from '../../shared/validation/validateurs';

/** Création d'un compte patient (POST /api/auth/register/patient). */
@Component({
  selector: 'app-inscription-patient',
  imports: [
    ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatDatepickerModule,
    MatCheckboxModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule,
  ],
  templateUrl: './inscription-patient.component.html',
  styleUrl: './inscription-patient.component.scss',
})
export class InscriptionPatientComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly sexes = SEXES;
  protected readonly groupesSanguins = GROUPES_SANGUINS;
  protected readonly aujourdhui = new Date();
  protected readonly debutCalendrier = new Date(1990, 0, 1);
  protected readonly messageChamp = messageChamp;

  protected readonly form = this.fb.group(
    {
      nom: ['', [Validators.required, Validators.maxLength(100)]],
      prenom: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      motDePasse: ['', [Validators.required, motDePasseValidator()]],
      confirmation: ['', Validators.required],
      dateNaissance: this.fb.control<Date | null>(null, dateDansLePasseValidator()),
      sexe: this.fb.control<Sexe | null>(null),
      groupeSanguin: this.fb.control<GroupeSanguin | null>(null),
      telephone: ['', telephoneValidator()],
      accepteConditions: [false, Validators.requiredTrue],
    },
    { validators: confirmationIdentiqueValidator() },
  );

  protected readonly envoiEnCours = signal(false);
  protected readonly erreur = signal<string | null>(null);
  protected readonly motDePasseMasque = signal(true);

  sInscrire(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.envoiEnCours.set(true);
    this.erreur.set(null);

    const v = this.form.getRawValue();
    this.auth
      .inscrirePatient({
        nom: v.nom.trim(),
        prenom: v.prenom.trim(),
        email: v.email.trim(),
        motDePasse: v.motDePasse,
        dateNaissance: versDateIso(v.dateNaissance),
        sexe: v.sexe,
        groupeSanguin: v.groupeSanguin,
        telephone: v.telephone.trim() || null,
      })
      .subscribe({
        next: () => this.router.navigateByUrl('/patient'),
        error: (err: HttpErrorResponse) => {
          this.envoiEnCours.set(false);
          if (err.status === 409) {
            // Email déjà utilisé : on l'affiche directement sous le champ email
            this.form.controls.email.setErrors({ serveur: messageErreurApi(err) });
            this.form.controls.email.markAsTouched();
          } else if (!appliquerErreursChamps(this.form, err)) {
            this.erreur.set(messageErreurApi(err));
          }
        },
      });
  }
}