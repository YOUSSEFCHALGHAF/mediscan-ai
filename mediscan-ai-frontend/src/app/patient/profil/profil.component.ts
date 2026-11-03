import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';

import { GroupeSanguin, GROUPES_SANGUINS, Sexe, SEXES, Utilisateur } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { PatientService } from '../../core/services/patient.service';
import { depuisDateIso, versDateIso } from '../../core/utils/dates';
import { appliquerErreursChamps, messageErreurApi } from '../../core/utils/erreurs-api';
import { messageChamp } from '../../shared/validation/messages-erreur';
import { dateDansLePasseValidator, telephoneValidator } from '../../shared/validation/validateurs';

/** Consultation et modification du profil patient (GET / PUT /api/patient/profil). */
@Component({
  selector: 'app-profil',
  imports: [
    DatePipe, ReactiveFormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatDatepickerModule,
    MatButtonModule, MatIconModule, MatProgressBarModule,
  ],
  templateUrl: './profil.component.html',
  styleUrl: './profil.component.scss',
})
export class ProfilComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly patientService = inject(PatientService);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly sexes = SEXES;
  protected readonly groupesSanguins = GROUPES_SANGUINS;
  protected readonly aujourdhui = new Date();
  protected readonly messageChamp = messageChamp;

  protected readonly chargement = signal(true);
  protected readonly enregistrement = signal(false);
  protected readonly erreur = signal<string | null>(null);
  protected readonly profil = signal<Utilisateur | null>(null);

  protected readonly form = this.fb.group({
    prenom: ['', [Validators.required, Validators.maxLength(100)]],
    nom: ['', [Validators.required, Validators.maxLength(100)]],
    dateNaissance: this.fb.control<Date | null>(null, dateDansLePasseValidator()),
    sexe: this.fb.control<Sexe | null>(null),
    groupeSanguin: this.fb.control<GroupeSanguin | null>(null),
    telephone: ['', telephoneValidator()],
  });

  ngOnInit(): void {
    this.patientService.getProfil().subscribe({
      next: (p) => {
        this.remplirFormulaire(p);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreurApi(err, 'Impossible de charger votre profil.'));
        this.chargement.set(false);
      },
    });
  }

  enregistrer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enregistrement.set(true);
    this.erreur.set(null);

    const v = this.form.getRawValue();
    this.patientService
      .modifierProfil({
        nom: v.nom.trim(),
        prenom: v.prenom.trim(),
        dateNaissance: versDateIso(v.dateNaissance),
        sexe: v.sexe,
        groupeSanguin: v.groupeSanguin,
        telephone: v.telephone.trim() || null,
      })
      .subscribe({
        next: (p) => {
          this.remplirFormulaire(p);
          this.auth.mettreAJourUtilisateur(p); // met à jour le nom dans la barre du haut
          this.enregistrement.set(false);
          this.snackBar.open('Profil enregistré', 'OK', { duration: 3000 });
        },
        error: (err) => {
          this.enregistrement.set(false);
          if (!appliquerErreursChamps(this.form, err)) {
            this.erreur.set(messageErreurApi(err));
          }
        },
      });
  }

  /** Annule les modifications non enregistrées. */
  annuler(): void {
    const p = this.profil();
    if (p) {
      this.remplirFormulaire(p);
    }
  }

  private remplirFormulaire(p: Utilisateur): void {
    this.profil.set(p);
    this.form.reset({
      prenom: p.prenom,
      nom: p.nom,
      dateNaissance: depuisDateIso(p.dateNaissance),
      sexe: p.sexe ?? null,
      groupeSanguin: p.groupeSanguin ?? null,
      telephone: p.telephone ?? '',
    });
  }
}