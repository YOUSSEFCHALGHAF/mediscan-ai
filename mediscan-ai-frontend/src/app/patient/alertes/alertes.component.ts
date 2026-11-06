import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { Alerte } from '../../core/models';
import { AlerteService } from '../../core/services/alerte.service';
import { messageErreurApi } from '../../core/utils/erreurs-api';
import { ListeAlertesComponent } from '../../shared/components/liste-alertes/liste-alertes.component';

/** Page "Mes alertes" du patient : toutes ses alertes, les non traitées en premier. */
@Component({
  selector: 'app-alertes',
  imports: [MatIconModule, MatProgressBarModule, ListeAlertesComponent],
  template: `
    <h1 class="titre-page">Mes alertes</h1>
    <p class="texte-secondaire intro">
      <mat-icon>info</mat-icon>
      Ces alertes sont calculées automatiquement à partir de vos scans et de votre anamnèse.
      Une alerte est marquée « traitée » quand votre médecin en a pris connaissance.
    </p>

    @if (chargement()) {
      <mat-progress-bar mode="indeterminate" />
    } @else if (erreur()) {
      <p class="bandeau-erreur" role="alert"><mat-icon>error</mat-icon> {{ erreur() }}</p>
    } @else {
      <p class="resume">
        <strong>{{ nonTraitees() }}</strong> alerte(s) en attente · {{ alertes().length }} au total
      </p>
      <app-liste-alertes
        [alertes]="alertes()"
        lienScans="/patient/historique"
        messageVide="Aucune alerte : rien à signaler dans vos médicaments scannés."
      />
    }
  `,
  styles: `
    .intro {
      display: flex;
      gap: 8px;
      align-items: flex-start;
    }
    .resume {
      margin: 16px 0;
    }
    .bandeau-erreur {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px;
      border-radius: 8px;
      background: var(--mat-sys-error-container);
      color: var(--mat-sys-on-error-container);
    }
  `,
})
export class AlertesComponent implements OnInit {
  private readonly alerteService = inject(AlerteService);

  protected readonly alertes = signal<Alerte[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly nonTraitees = computed(() => this.alertes().filter((a) => !a.traitee).length);

  ngOnInit(): void {
    // getMesAlertes met aussi à jour la pastille du menu
    this.alerteService.getMesAlertes().subscribe({
      next: (alertes) => {
        this.alertes.set(alertes);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreurApi(err, 'Impossible de charger vos alertes.'));
        this.chargement.set(false);
      },
    });
  }
}