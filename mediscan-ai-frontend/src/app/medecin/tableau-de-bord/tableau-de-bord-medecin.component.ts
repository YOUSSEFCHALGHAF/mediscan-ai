import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';

import {
  configEvolution,
  configMedicaments,
  configRepartition,
  lireCouleursGravite,
  resumeEvolution,
  resumeMedicaments,
  resumeRepartition,
} from '../../core/graphiques/configurations';
import { AlerteATraiter, TableauDeBordMedecin } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { MedecinService } from '../../core/services/medecin.service';
import { messageErreurApi } from '../../core/utils/erreurs-api';
import { GraphiqueComponent } from '../../shared/components/graphique/graphique.component';
import { FileAlertesComponent } from '../file-alertes/file-alertes.component';

/**
 * Tableau de bord du médecin : indicateurs clés, graphiques sur 30 jours, file des alertes à traiter.
 * Toutes les données sont limitées par le serveur aux patients ayant donné leur consentement,
 * et au périmètre qu'ils ont choisi de partager.
 */
@Component({
  selector: 'app-tableau-de-bord-medecin',
  imports: [RouterLink, MatCardModule, MatIconModule, MatButtonModule, MatProgressBarModule, GraphiqueComponent, FileAlertesComponent],
  templateUrl: './tableau-de-bord-medecin.component.html',
  styleUrl: './tableau-de-bord-medecin.component.scss',
})
export class TableauDeBordMedecinComponent implements OnInit {
  private readonly medecinService = inject(MedecinService);
  protected readonly auth = inject(AuthService);

  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly donnees = signal<TableauDeBordMedecin | null>(null);

  private readonly couleurs = lireCouleursGravite();

  protected readonly configEvolution = computed(() => configEvolution(this.donnees()?.evolutionAlertes ?? [], this.couleurs));
  protected readonly configRepartition = computed(() => configRepartition(this.donnees()?.repartitionParType ?? []));
  protected readonly configMedicaments = computed(() => configMedicaments(this.donnees()?.medicamentsLesPlusScannes ?? []));

  protected readonly resumeEvolution = computed(() => resumeEvolution(this.donnees()?.evolutionAlertes ?? []));
  protected readonly resumeRepartition = computed(() => resumeRepartition(this.donnees()?.repartitionParType ?? []));
  protected readonly resumeMedicaments = computed(() => resumeMedicaments(this.donnees()?.medicamentsLesPlusScannes ?? []));

  /** Un anneau "vide" n'apporte rien : on affiche un message à la place. */
  protected readonly aDesAlertesSurLaPeriode = computed(() =>
    (this.donnees()?.repartitionParType ?? []).some((c) => c.nombre > 0),
  );

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set(null);
    this.medecinService.getTableauDeBord().subscribe({
      next: (donnees) => {
        this.donnees.set(donnees);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreurApi(err, 'Impossible de charger votre tableau de bord.'));
        this.chargement.set(false);
      },
    });
  }

  /** Alerte traitée depuis la file : mise à jour immédiate des indicateurs, sans recharger. */
  protected surTraitee(alerte: AlerteATraiter): void {
    this.donnees.update((d) => {
      if (!d) {
        return d;
      }
      return {
        ...d,
        alertesATraiter: d.alertesATraiter.filter((a) => a.id !== alerte.id),
        indicateurs: {
          ...d.indicateurs,
          alertesNonTraitees: Math.max(0, d.indicateurs.alertesNonTraitees - 1),
          alertesElevees: Math.max(0, d.indicateurs.alertesElevees - (alerte.niveauGravite === 'ELEVE' ? 1 : 0)),
        },
      };
    });
  }
}