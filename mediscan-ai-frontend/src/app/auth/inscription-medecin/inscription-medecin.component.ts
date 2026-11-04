import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { appliquerErreursChamps, messageErreurApi } from '../../core/utils/erreurs-api';
import { normaliser } from '../../core/utils/texte';
import { messageChamp } from '../../shared/validation/messages-erreur';
import {
  confirmationIdentiqueValidator,
  motDePasseValidator,
  rppsValidator,
  telephoneValidator,
} from '../../shared/validation/validateurs';

/** Spécialités proposées (le médecin peut aussi en saisir une autre). */
const SPECIALITES = [
  'Médecine générale', 'Cardiologie', 'Dermatologie', 'Endocrinologie', 'Gastro-entérologie',
  'Gériatrie', 'Gynécologie', 'Neurologie', 'Oncologie', 'Ophtalmologie', 'ORL', 'Pédiatrie',
  'Pneumologie', 'Psychiatrie', 'Rhumatologie', 'Urologie',
];

/** Création d'un compte médecin (POST /api/auth/register/medecin). */
@Component({
  selector: 'app-inscription-medecin',
  imports: [
    ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule, MatAutocompleteModule, MatCheckboxModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
  ],
  templateUrl: './inscription-medecin.component.html',
  styleUrl: './inscription-medecin.component.scss',
})
export class InscriptionMedecinComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly messageChamp = messageChamp;

  protected readonly form = this.fb.group(
    {
      prenom: ['', [Validators.required, Validators.maxLength(100)]],
      nom: ['', [Validators.required, Validators.maxLength(100)]],
      numeroRpps: ['', [Validators.required, rppsValidator()]],
      specialite: ['', [Validators.required, Validators.maxLength(100)]],
      telephone: ['', telephoneValidator()],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      motDePasse: ['', [Validators.required, motDePasseValidator()]],
      confirmation: ['', Validators.required],
      attestation: [false, Validators.requiredTrue],
    },
    { validators: confirmationIdentiqueValidator() },
  );

  /** Saisie de la spécialité, transformée en signal pour filtrer l'autocomplétion. */
  private readonly saisieSpecialite = toSignal(this.form.controls.specialite.valueChanges, { initialValue: '' });
  protected readonly specialitesFiltrees = computed(() => {
    const saisie = normaliser(this.saisieSpecialite());
    return SPECIALITES.filter((s) => normaliser(s).includes(saisie));
  });

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
      .inscrireMedecin({
        nom: v.nom.trim(),
        prenom: v.prenom.trim(),
        email: v.email.trim(),
        motDePasse: v.motDePasse,
        numeroRpps: v.numeroRpps.trim(),
        specialite: v.specialite.trim(),
        telephone: v.telephone.trim() || null,
      })
      .subscribe({
        next: () => this.router.navigateByUrl('/medecin'),
        error: (err: HttpErrorResponse) => {
          this.envoiEnCours.set(false);
          if (err.status === 409) {
            // 409 : email OU numéro RPPS déjà utilisé -> on place l'erreur sur le bon champ
            const message = messageErreurApi(err);
            const champ = message.includes('RPPS') ? this.form.controls.numeroRpps : this.form.controls.email;
            champ.setErrors({ serveur: message });
            champ.markAsTouched();
          } else if (!appliquerErreursChamps(this.form, err)) {
            this.erreur.set(messageErreurApi(err));
          }
        },
      });
  }
}