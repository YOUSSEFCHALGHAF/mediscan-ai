import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Anamnese, LIBELLES_STATUT_SCAN, Rappel, Scan } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { PatientService } from '../../core/services/patient.service';
import { heureCourte } from '../../core/utils/dates';
import { messageErreurApi } from '../../core/utils/erreurs-api';

/** Données chargées en parallèle pour le tableau de bord. */
interface DonneesTableauDeBord {
  anamnese: Anamnese;
  scans: Scan[];
  rappels: Rappel[];
}

/** Vue d'ensemble du patient : anamnèse, derniers scans, prochain rappel. */
@Component({
  selector: 'app-tableau-de-bord',
  imports: [DatePipe, RouterLink, MatCardModule, MatIconModule, MatButtonModule, MatProgressBarModule],
  templateUrl: './tableau-de-bord.component.html',
  styleUrl: './tableau-de-bord.component.scss',
})
export class TableauDeBordComponent implements OnInit {
  private readonly patientService = inject(PatientService);
  protected readonly auth = inject(AuthService);

  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly donnees = signal<DonneesTableauDeBord | null>(null);
  protected readonly libellesStatut = LIBELLES_STATUT_SCAN;
  protected readonly heureCourte = heureCourte;

  protected readonly rappelsActifs = computed(() => this.donnees()?.rappels.filter((r) => r.actif) ?? []);

  /** Prochain rappel : le premier après l'heure actuelle, sinon le premier de demain. */
  protected readonly prochainRappel = computed<Rappel | null>(() => {
    const actifs = [...this.rappelsActifs()].sort((a, b) => a.heurePrise.localeCompare(b.heurePrise));
    if (!actifs.length) {
      return null;
    }
    const maintenant = new Date().toTimeString().substring(0, 8); // "HH:mm:ss"
    return actifs.find((r) => r.heurePrise > maintenant) ?? actifs[0];
  });

  protected readonly derniersScans = computed(() => this.donnees()?.scans.slice(0, 3) ?? []);

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set(null);
    // forkJoin : lance les 3 requêtes EN PARALLÈLE et attend qu'elles soient toutes terminées
    forkJoin({
      anamnese: this.patientService.getAnamnese(),
      scans: this.patientService.getScans(),
      rappels: this.patientService.getRappels(),
    }).subscribe({
      next: (resultat) => {
        this.donnees.set(resultat);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreurApi(err, 'Impossible de charger votre tableau de bord.'));
        this.chargement.set(false);
      },
    });
  }
}