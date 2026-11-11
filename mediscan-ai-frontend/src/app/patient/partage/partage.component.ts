import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { filter, switchMap } from 'rxjs';

import { LIBELLES_CATEGORIE, Partage } from '../../core/models';
import { MessagesService } from '../../core/services/messages.service';
import { PartageService } from '../../core/services/partage.service';
import { messageErreurApi } from '../../core/utils/erreurs-api';
import { demanderConfirmation } from '../../shared/components/dialogue-confirmation/dialogue-confirmation.component';
import { DonneesPartage, PartageDialogueComponent } from './partage-dialogue/partage-dialogue.component';

/**
 * Page "Partage médecin" : le patient décide qui voit quoi.
 * - accès actifs : modifier les catégories, révoquer ;
 * - donner accès à un nouveau médecin ;
 * - historique des accès révoqués (traçabilité).
 */
@Component({
  selector: 'app-partage',
  imports: [DatePipe, MatCardModule, MatButtonModule, MatChipsModule, MatExpansionModule, MatIconModule, MatProgressBarModule],
  templateUrl: './partage.component.html',
  styleUrl: './partage.component.scss',
})
export class PartageComponent implements OnInit {
  private readonly partageService = inject(PartageService);
  private readonly messages = inject(MessagesService);
  private readonly dialog = inject(MatDialog);

  protected readonly libellesCategorie = LIBELLES_CATEGORIE;
  protected readonly partages = signal<Partage[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);

  protected readonly actifs = computed(() => this.partages().filter((p) => p.actif));
  protected readonly revoques = computed(() => this.partages().filter((p) => !p.actif));
  protected readonly libelleDe = (c: keyof typeof LIBELLES_CATEGORIE) => LIBELLES_CATEGORIE[c];

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.partageService.lister().subscribe({
      next: (partages) => {
        this.partages.set(partages);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreurApi(err, 'Impossible de charger vos partages.'));
        this.chargement.set(false);
      },
    });
  }

  /** Nouveau partage, ou modification des catégories d'un partage actif. */
  ouvrir(partage: Partage | null = null): void {
    this.dialog
      .open<PartageDialogueComponent, DonneesPartage, Partage>(PartageDialogueComponent, {
        data: { partage, medecinsAutorises: this.actifs().map((p) => p.medecin.id) },
        width: '600px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .pipe(filter((resultat): resultat is Partage => !!resultat))
      .subscribe((resultat) => {
        this.messages.succes(
          partage
            ? `Accès du Dr ${resultat.medecin.nom} mis à jour`
            : `Le Dr ${resultat.medecin.nom} a désormais accès aux données choisies`,
        );
        // Une modification révoque l'ancien consentement et en crée un nouveau : on recharge l'historique
        this.charger();
      });
  }

  revoquer(partage: Partage): void {
    const medecin = `Dr ${partage.medecin.prenom} ${partage.medecin.nom}`;
    demanderConfirmation(this.dialog, {
      titre: `Retirer l'accès du ${medecin} ?`,
      message: `${medecin} ne pourra plus consulter vos données, dès maintenant. `
        + 'Cette décision est enregistrée dans votre historique de partages. Vous pourrez lui redonner accès plus tard.',
      libelleConfirmer: 'Retirer l\'accès',
    })
      .pipe(
        filter((confirme) => confirme),
        switchMap(() => this.partageService.revoquer(partage.id)),
      )
      .subscribe({
        next: () => {
          this.messages.succes(`Accès du ${medecin} retiré`);
          this.charger();
        },
        error: (err) => this.messages.erreur(messageErreurApi(err)),
      });
  }
}