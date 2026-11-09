import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { filter, switchMap } from 'rxjs';

import { LIBELLES_FREQUENCE, Rappel, RappelRequest } from '../../core/models';
import { NotificationPushService } from '../../core/notifications/notification-push.service';
import { MessagesService } from '../../core/services/messages.service';
import { PatientService } from '../../core/services/patient.service';
import { heureCourte } from '../../core/utils/dates';
import { messageErreurApi } from '../../core/utils/erreurs-api';
import { demanderConfirmation } from '../../shared/components/dialogue-confirmation/dialogue-confirmation.component';
import { DonneesRappel, RappelDialogueComponent } from './rappel-dialogue/rappel-dialogue.component';

/** Rappels triés par heure de prise. */
function trier(rappels: Rappel[]): Rappel[] {
  return [...rappels].sort((a, b) => a.heurePrise.localeCompare(b.heurePrise));
}

/**
 * Page "Mes rappels" : création, modification, activation/désactivation, suppression.
 * Le serveur envoie une notification à l'heure de chaque rappel actif (étape 17).
 */
@Component({
  selector: 'app-rappels',
  imports: [DatePipe, MatCardModule, MatButtonModule, MatIconModule, MatProgressBarModule, MatSlideToggleModule],
  templateUrl: './rappels.component.html',
  styleUrl: './rappels.component.scss',
})
export class RappelsComponent implements OnInit {
  private readonly patientService = inject(PatientService);
  private readonly messages = inject(MessagesService);
  private readonly dialog = inject(MatDialog);
  protected readonly notifications = inject(NotificationPushService);

  protected readonly libellesFrequence = LIBELLES_FREQUENCE;
  protected readonly heureCourte = heureCourte;
  protected readonly rappels = signal<Rappel[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly enCours = signal<ReadonlySet<number>>(new Set());

  ngOnInit(): void {
    this.patientService.getRappels().subscribe({
      next: (rappels) => {
        this.rappels.set(trier(rappels));
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreurApi(err, 'Impossible de charger vos rappels.'));
        this.chargement.set(false);
      },
    });
  }

  /** Création (rappel null) ou modification. */
  ouvrir(rappel: Rappel | null = null): void {
    this.dialog
      .open<RappelDialogueComponent, DonneesRappel, Rappel>(RappelDialogueComponent, {
        data: { rappel },
        width: '560px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .pipe(filter((resultat): resultat is Rappel => !!resultat))
      .subscribe((enregistre) => {
        this.remplacer(enregistre);
        this.messages.succes(rappel ? 'Rappel modifié' : 'Rappel créé');
      });
  }

  /** Active ou met en pause un rappel (sans le supprimer). */
  basculer(rappel: Rappel, actif: boolean): void {
    this.marquer(rappel.id, true);
    this.patientService.modifierRappel(rappel.id, this.requeteDepuis(rappel, actif)).subscribe({
      next: (modifie) => {
        this.remplacer(modifie);
        this.marquer(rappel.id, false);
        this.messages.succes(actif ? 'Rappel réactivé' : 'Rappel mis en pause');
      },
      error: (err) => {
        this.marquer(rappel.id, false);
        this.remplacer({ ...rappel }); // remet l'interrupteur dans son état réel
        this.messages.erreur(messageErreurApi(err));
      },
    });
  }

  supprimer(rappel: Rappel): void {
    demanderConfirmation(this.dialog, {
      titre: 'Supprimer ce rappel ?',
      message: `Le rappel « ${rappel.libelle} » (${heureCourte(rappel.heurePrise)}) sera supprimé.`,
      libelleConfirmer: 'Supprimer',
    })
      .pipe(
        filter((confirme) => confirme),
        switchMap(() => this.patientService.supprimerRappel(rappel.id)),
      )
      .subscribe({
        next: () => {
          this.rappels.update((liste) => liste.filter((r) => r.id !== rappel.id));
          this.messages.succes('Rappel supprimé');
        },
        error: (err) => this.messages.erreur(messageErreurApi(err)),
      });
  }

  protected activerNotifications(): void {
    void this.notifications.activer();
  }

  private requeteDepuis(r: Rappel, actif: boolean): RappelRequest {
    return {
      medicamentId: r.medicament.id,
      libelle: r.libelle,
      heurePrise: heureCourte(r.heurePrise),
      frequence: r.frequence,
      dateDebut: r.dateDebut,
      dateFin: r.dateFin,
      actif,
    };
  }

  private remplacer(rappel: Rappel): void {
    this.rappels.update((liste) => trier([...liste.filter((r) => r.id !== rappel.id), rappel]));
  }

  private marquer(id: number, actif: boolean): void {
    this.enCours.update((ids) => {
      const copie = new Set(ids);
      if (actif) {
        copie.add(id);
      } else {
        copie.delete(id);
      }
      return copie;
    });
  }
}