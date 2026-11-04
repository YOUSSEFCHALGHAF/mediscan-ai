import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';

import { PatientResume } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { MedecinService } from '../../core/services/medecin.service';
import { messageErreurApi } from '../../core/utils/erreurs-api';
import { PastilleAlertesComponent } from '../../shared/components/pastille-alertes/pastille-alertes.component';

/**
 * Tableau de bord du médecin (version de l'étape 10).
 * Indicateurs calculés à partir de la liste des patients ; graphiques et traitement
 * des alertes seront ajoutés à l'étape 18.
 */
@Component({
  selector: 'app-tableau-de-bord-medecin',
  imports: [RouterLink, MatCardModule, MatIconModule, MatButtonModule, MatListModule, MatProgressBarModule, PastilleAlertesComponent],
  templateUrl: './tableau-de-bord-medecin.component.html',
  styleUrl: './tableau-de-bord-medecin.component.scss',
})
export class TableauDeBordMedecinComponent implements OnInit {
  private readonly medecinService = inject(MedecinService);
  protected readonly auth = inject(AuthService);

  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly patients = signal<PatientResume[]>([]);

  protected readonly totalAlertes = computed(() =>
    this.patients().reduce((somme, p) => somme + p.alertesNonTraitees, 0),
  );
  /** Patients avec au moins une alerte, du plus au moins concerné (5 maximum). */
  protected readonly aSurveiller = computed(() =>
    this.patients()
      .filter((p) => p.alertesNonTraitees > 0)
      .sort((a, b) => b.alertesNonTraitees - a.alertesNonTraitees)
      .slice(0, 5),
  );

  ngOnInit(): void {
    this.medecinService.getPatients().subscribe({
      next: (liste) => {
        this.patients.set(liste);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreurApi(err, 'Impossible de charger votre tableau de bord.'));
        this.chargement.set(false);
      },
    });
  }
}