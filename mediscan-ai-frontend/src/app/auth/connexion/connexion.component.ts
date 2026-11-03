import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/services/auth.service';
import { messageErreurApi } from '../../core/utils/erreurs-api';
import { messageChamp } from '../../shared/validation/messages-erreur';

/** Page de connexion commune aux patients et aux médecins. */
@Component({
  selector: 'app-connexion',
  imports: [
    ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule,
  ],
  templateUrl: './connexion.component.html',
  styleUrl: './connexion.component.scss',
})
export class ConnexionComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    motDePasse: ['', Validators.required],
  });

  protected readonly envoiEnCours = signal(false);
  protected readonly erreur = signal<string | null>(null);
  protected readonly motDePasseMasque = signal(true);
  protected readonly sessionExpiree = this.route.snapshot.queryParamMap.get('raison') === 'expiree';
  protected readonly messageChamp = messageChamp;

  /** Comptes de démonstration (affichés en développement uniquement). */
  protected readonly comptesDemo = environment.production
    ? []
    : [
        { libelle: 'Marc (patient)', email: 'marc.lefevre@mediscan.test' },
        { libelle: 'Yasmine (patiente)', email: 'yasmine.haddad@mediscan.test' },
        { libelle: 'Dr Mansour (médecin)', email: 'karim.mansour@mediscan.test' },
      ];

  remplirCompteDemo(email: string): void {
    this.form.setValue({ email, motDePasse: 'MediScan2026!' });
  }

  seConnecter(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.envoiEnCours.set(true);
    this.erreur.set(null);

    const { email, motDePasse } = this.form.getRawValue();
    this.auth.connecter({ email: email.trim(), motDePasse }).subscribe({
      next: (reponse) => {
        const espace = this.auth.cheminEspace(reponse.utilisateur.role);
        // Retour à la page demandée uniquement si elle appartient au bon espace
        // (évite toute redirection vers une page interdite ou un site externe)
        const retour = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(retour?.startsWith(espace) ? retour : espace);
      },
      error: (err) => {
        this.erreur.set(messageErreurApi(err));
        this.envoiEnCours.set(false);
      },
    });
  }
}