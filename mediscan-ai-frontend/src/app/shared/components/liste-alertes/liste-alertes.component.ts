import { DatePipe, LowerCasePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { Alerte, LIBELLES_GRAVITE, LIBELLES_TYPE_ALERTE } from '../../../core/models';
import { ICONES_TYPE_ALERTE, trierAlertes } from '../../../core/utils/alertes';

/**
 * Liste d'alertes médicamenteuses, triées (non traitées et graves d'abord).
 * Utilisée par le patient (résultat de scan, page Mes alertes) et le médecin (fiche patient).
 */
@Component({
  selector: 'app-liste-alertes',
  imports: [DatePipe, LowerCasePipe, RouterLink, MatIconModule, MatButtonModule],
  templateUrl: './liste-alertes.component.html',
  styleUrl: './liste-alertes.component.scss',
})
export class ListeAlertesComponent {
  readonly alertes = input.required<Alerte[]>();
  /** Médecin : affiche le bouton "Marquer comme traitée". */
  readonly peutTraiter = input(false);
  /** Identifiants des alertes dont le traitement est en cours d'envoi (bouton désactivé). */
  readonly enCours = input<ReadonlySet<number>>(new Set());
  /** Lien vers le détail des scans (ex. "/patient/historique") ; null = pas de lien. */
  readonly lienScans = input<string | null>(null);
  readonly messageVide = input('Aucune alerte.');

  readonly traiter = output<Alerte>();

  protected readonly triees = computed(() => trierAlertes(this.alertes()));
  protected readonly libellesType = LIBELLES_TYPE_ALERTE;
  protected readonly libellesGravite = LIBELLES_GRAVITE;
  protected readonly icones = ICONES_TYPE_ALERTE;
}