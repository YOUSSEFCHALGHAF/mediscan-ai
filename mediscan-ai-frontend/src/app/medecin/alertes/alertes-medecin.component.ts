import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { AlerteATraiter } from '../../core/models';
import { MedecinService } from '../../core/services/medecin.service';
import { messageErreurApi } from '../../core/utils/erreurs-api';
import { FileAlertesComponent } from '../file-alertes/file-alertes.component';

/** Page "Alertes" du médecin : toutes les alertes non traitées de ses patients. */
@Component({
  selector: 'app-alertes-medecin',
  imports: [MatIconModule, MatProgressBarModule, FileAlertesComponent],
  template: `
    <h1 class="titre-page">Alertes à traiter</h1>
    @if (chargement()) {
      <mat-progress-bar mode="indeterminate" />
    } @else if (erreur()) {
      <p class="erreur" role="alert"><mat-icon>error</mat-icon> {{ erreur() }}</p>
    } @else {
      <p class="resume">
        <strong>{{ alertes().length }}</strong> alerte(s) en attente
        @if (elevees()) { · <strong class="elevees">{{ elevees() }} de gravité élevée</strong> }
      </p>
      <app-file-alertes
        [alertes]="alertes()"
        messageVide="Aucune alerte en attente pour vos patients. 👍"
        (traitee)="retirer($event)"
      />
    }
  `,
  styles: `
    .resume {
      margin: 0 0 16px;
    }
    .elevees {
      color: var(--mat-sys-error);
    }
    .erreur {
      display: flex;
      align-items: center;
      gap: 8px;
      color: var(--mat-sys-error);
    }
  `,
})
export class AlertesMedecinComponent implements OnInit {
  private readonly medecinService = inject(MedecinService);

  protected readonly alertes = signal<AlerteATraiter[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly elevees = computed(() => this.alertes().filter((a) => a.niveauGravite === 'ELEVE').length);

  ngOnInit(): void {
    this.medecinService.getAlertesATraiter().subscribe({
      next: (alertes) => {
        this.alertes.set(alertes);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreurApi(err, 'Impossible de charger les alertes.'));
        this.chargement.set(false);
      },
    });
  }

  protected retirer(alerte: AlerteATraiter): void {
    this.alertes.update((liste) => liste.filter((a) => a.id !== alerte.id));
  }
}