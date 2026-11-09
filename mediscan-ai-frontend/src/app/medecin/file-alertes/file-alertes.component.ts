import { Component, inject, input, output, signal } from '@angular/core';

import { Alerte, AlerteATraiter } from '../../core/models';
import { AlerteService } from '../../core/services/alerte.service';
import { MessagesService } from '../../core/services/messages.service';
import { messageErreurApi } from '../../core/utils/erreurs-api';
import { ListeAlertesComponent } from '../../shared/components/liste-alertes/liste-alertes.component';

/**
 * File des alertes à traiter, tous patients confondus (tableau de bord et page Alertes du médecin).
 * Le bouton "Marquer comme traitée" appelle l'API ; le parent retire l'alerte grâce à l'événement "traitee".
 */
@Component({
  selector: 'app-file-alertes',
  imports: [ListeAlertesComponent],
  template: `
    <app-liste-alertes
      [alertes]="alertes()"
      [peutTraiter]="true"
      [enCours]="enCours()"
      [messageVide]="messageVide()"
      (traiter)="traiter($event)"
    />
  `,
})
export class FileAlertesComponent {
  private readonly alerteService = inject(AlerteService);
  private readonly messages = inject(MessagesService);

  readonly alertes = input.required<AlerteATraiter[]>();
  readonly messageVide = input('Aucune alerte en attente.');
  /** Émis quand le serveur a confirmé le traitement. */
  readonly traitee = output<AlerteATraiter>();

  protected readonly enCours = signal<ReadonlySet<number>>(new Set());

  protected traiter(alerte: Alerte): void {
    const a = alerte as AlerteATraiter;
    this.enCours.update((ids) => new Set(ids).add(a.id));
    this.alerteService.marquerTraitee(a.patientId, a.id).subscribe({
      next: () => {
        this.retirer(a.id);
        this.messages.succes(`Alerte de ${a.patientNom} marquée comme traitée`);
        this.traitee.emit(a);
      },
      error: (err) => {
        this.retirer(a.id);
        this.messages.erreur(messageErreurApi(err));
      },
    });
  }

  private retirer(id: number): void {
    this.enCours.update((ids) => {
      const copie = new Set(ids);
      copie.delete(id);
      return copie;
    });
  }
}