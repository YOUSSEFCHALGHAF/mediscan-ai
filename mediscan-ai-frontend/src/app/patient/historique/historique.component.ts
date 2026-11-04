import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';

import { Scan } from '../../core/models';
import { PatientService } from '../../core/services/patient.service';
import { etatExpiration } from '../../core/utils/dates';
import { messageErreurApi } from '../../core/utils/erreurs-api';
import { AFFICHAGE_STATUT } from '../../core/utils/scan';
import { ImageScanComponent } from '../../shared/components/image-scan/image-scan.component';

type Filtre = 'TOUS' | 'TERMINE' | 'A_VERIFIER' | 'ECHEC';

const FILTRES: { valeur: Filtre; libelle: string }[] = [
  { valeur: 'TOUS', libelle: 'Tous' },
  { valeur: 'TERMINE', libelle: 'Reconnus' },
  { valeur: 'A_VERIFIER', libelle: 'À vérifier' },
  { valeur: 'ECHEC', libelle: 'Échecs' },
];

/**
 * Historique des scans du patient : miniatures, filtres par statut, accès au détail.
 * Le filtre peut être imposé par l'URL : /patient/historique?statut=A_VERIFIER
 */
@Component({
  selector: 'app-historique',
  imports: [DatePipe, RouterLink, MatButtonModule, MatChipsModule, MatIconModule, MatProgressBarModule, ImageScanComponent],
  templateUrl: './historique.component.html',
  styleUrl: './historique.component.scss',
})
export class HistoriqueComponent implements OnInit {
  private readonly patientService = inject(PatientService);

  /** Paramètre d'URL facultatif ?statut=... (relié automatiquement par withComponentInputBinding). */
  readonly statut = input<string>();

  protected readonly filtres = FILTRES;
  protected readonly affichageStatut = AFFICHAGE_STATUT;
  protected readonly etatExpiration = etatExpiration;

  protected readonly scans = signal<Scan[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly filtre = signal<Filtre>('TOUS');

  /** Nombre de scans par filtre (affiché dans les pastilles). */
  protected readonly compteurs = computed(() => {
    const liste = this.scans();
    return {
      TOUS: liste.length,
      TERMINE: liste.filter((s) => s.statut === 'TERMINE').length,
      A_VERIFIER: liste.filter((s) => s.statut === 'A_VERIFIER' || s.statut === 'EN_COURS').length,
      ECHEC: liste.filter((s) => s.statut === 'ECHEC').length,
    } satisfies Record<Filtre, number>;
  });

  protected readonly scansFiltres = computed(() => {
    const f = this.filtre();
    return this.scans().filter((s) =>
      f === 'TOUS' ? true : f === 'A_VERIFIER' ? s.statut === 'A_VERIFIER' || s.statut === 'EN_COURS' : s.statut === f,
    );
  });

  ngOnInit(): void {
    const demande = this.statut();
    if (FILTRES.some((f) => f.valeur === demande)) {
      this.filtre.set(demande as Filtre);
    }
    this.patientService.getScans().subscribe({
      next: (scans) => {
        this.scans.set(scans);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreurApi(err, 'Impossible de charger votre historique.'));
        this.chargement.set(false);
      },
    });
  }

  protected choisirFiltre(valeur: Filtre | undefined): void {
    this.filtre.set(valeur ?? 'TOUS');
  }
}