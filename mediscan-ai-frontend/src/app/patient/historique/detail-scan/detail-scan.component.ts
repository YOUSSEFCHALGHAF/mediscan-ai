import { DatePipe } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';

import { Medicament, Scan } from '../../../core/models';
import { MedicamentService } from '../../../core/services/medicament.service';
import { MessagesService } from '../../../core/services/messages.service';
import { PatientService } from '../../../core/services/patient.service';
import { ScanService } from '../../../core/services/scan.service';
import { messageErreurApi } from '../../../core/utils/erreurs-api';
import { demanderConfirmation } from '../../../shared/components/dialogue-confirmation/dialogue-confirmation.component';
import { ImageScanComponent } from '../../../shared/components/image-scan/image-scan.component';
import { ResultatScanComponent } from '../../../shared/components/resultat-scan/resultat-scan.component';
import { CorrectionMedicamentComponent, DonneesCorrection } from '../correction-medicament/correction-medicament.component';

/**
 * Détail d'un scan (/patient/historique/:scanId) :
 * photo, résultat de l'analyse, et actions (corriger, ajouter aux traitements, supprimer).
 */
@Component({
  selector: 'app-detail-scan',
  imports: [
    DatePipe, RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatProgressBarModule,
    ImageScanComponent, ResultatScanComponent,
  ],
  templateUrl: './detail-scan.component.html',
  styleUrl: './detail-scan.component.scss',
})
export class DetailScanComponent implements OnInit {
  private readonly patientService = inject(PatientService);
  private readonly scanService = inject(ScanService);
  private readonly medicamentService = inject(MedicamentService);
  private readonly messages = inject(MessagesService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  /** Paramètre de route :scanId (relié automatiquement par withComponentInputBinding). */
  readonly scanId = input.required<string>();

  protected readonly scan = signal<Scan | null>(null);
  protected readonly fiche = signal<Medicament | null>(null);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  /** Action en cours : désactive les boutons pour éviter les doubles clics. */
  protected readonly actionEnCours = signal(false);

  ngOnInit(): void {
    this.patientService.getScan(Number(this.scanId())).subscribe({
      next: (scan) => {
        this.afficher(scan);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(err.status === 404 ? 'Ce scan n\'existe pas ou a été supprimé.' : messageErreurApi(err));
        this.chargement.set(false);
      },
    });
  }

  /** Ouvre la recherche du bon médicament, puis enregistre la correction. */
  corriger(): void {
    const scan = this.scan();
    if (!scan) {
      return;
    }
    this.dialog
      .open<CorrectionMedicamentComponent, DonneesCorrection, Medicament>(CorrectionMedicamentComponent, {
        data: { scan },
        width: '520px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .pipe(
        filter((choix): choix is Medicament => !!choix),
        switchMap((medicament) => {
          this.actionEnCours.set(true);
          this.fiche.set(medicament); // fiche complète déjà connue grâce à la recherche
          return this.scanService.corrigerMedicament(scan.id, medicament.id);
        }),
      )
      .subscribe({
        next: (corrige) => {
          this.scan.set(corrige);
          this.actionEnCours.set(false);
          this.messages.succes(`Scan corrigé : ${corrige.medicament?.nomCommercial}`);
        },
        error: (err) => {
          this.actionEnCours.set(false);
          this.messages.erreur(messageErreurApi(err));
        },
      });
  }

  /** Ajoute la substance du médicament aux traitements en cours de l'anamnèse. */
  ajouterAuxTraitements(): void {
    const scan = this.scan();
    if (!scan) {
      return;
    }
    this.actionEnCours.set(true);
    this.scanService.ajouterAuxTraitements(scan.id).subscribe({
      next: (resultat) => {
        this.actionEnCours.set(false);
        this.messages.succes(
          resultat.dejaPresent
            ? `${resultat.substance} figure déjà dans vos traitements en cours`
            : `${resultat.substance} ajouté à vos traitements en cours`,
        );
      },
      error: (err) => {
        this.actionEnCours.set(false);
        this.messages.erreur(messageErreurApi(err));
      },
    });
  }

  /** Suppression définitive (après confirmation), puis retour à l'historique. */
  supprimer(): void {
    const scan = this.scan();
    if (!scan) {
      return;
    }
    demanderConfirmation(this.dialog, {
      titre: 'Supprimer ce scan ?',
      message: 'La photo et le résultat de l\'analyse seront définitivement effacés. Cette action est irréversible.',
      libelleConfirmer: 'Supprimer définitivement',
      libelleAnnuler: 'Annuler',
    })
      .pipe(
        filter((confirme) => confirme),
        switchMap(() => {
          this.actionEnCours.set(true);
          return this.scanService.supprimer(scan.id);
        }),
      )
      .subscribe({
        next: () => {
          this.messages.succes('Scan supprimé');
          void this.router.navigate(['..'], { relativeTo: this.route });
        },
        error: (err) => {
          this.actionEnCours.set(false);
          this.messages.erreur(messageErreurApi(err));
        },
      });
  }

  private afficher(scan: Scan): void {
    this.scan.set(scan);
    if (scan.medicament) {
      this.medicamentService.getMedicament(scan.medicament.id).subscribe({
        next: (fiche) => this.fiche.set(fiche),
        error: () => this.fiche.set(null),
      });
    }
  }
}