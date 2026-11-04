import { HttpErrorResponse, HttpEventType } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';

import { Medicament, Scan } from '../../core/models';
import { CompressionImageService } from '../../core/services/compression-image.service';
import { MedicamentService } from '../../core/services/medicament.service';
import { MessagesService } from '../../core/services/messages.service';
import { ScanService } from '../../core/services/scan.service';
import { cameraDisponible } from '../../core/utils/camera';
import { messageErreurApi } from '../../core/utils/erreurs-api';
import { formaterTaille, verifierFichierImage } from '../../core/utils/image';
import { ResultatScanComponent } from '../../shared/components/resultat-scan/resultat-scan.component';
import { CaptureWebcamComponent } from './capture-webcam/capture-webcam.component';

type Etape = 'choix' | 'apercu' | 'envoi' | 'termine';
type Mode = 'fichier' | 'webcam';

/**
 * Page "Scanner un médicament" :
 * 1. choix : importer une photo (fichier, glisser-déposer, appareil photo du téléphone) ou webcam ;
 * 2. aperçu : vérifier la photo (compressée à 1600 px) ;
 * 3. envoi : barre de progression ;
 * 4. terminé : résultat de l'analyse IA (médicament reconnu, lot, expiration...).
 */
@Component({
  selector: 'app-scanner',
  imports: [
    DatePipe, RouterLink,
    MatCardModule, MatButtonModule, MatButtonToggleModule, MatIconModule, MatProgressBarModule,
    CaptureWebcamComponent, ResultatScanComponent,
  ],
  templateUrl: './scanner.component.html',
  styleUrl: './scanner.component.scss',
})
export class ScannerComponent {
  private readonly scanService = inject(ScanService);
  private readonly medicamentService = inject(MedicamentService);
  private readonly compression = inject(CompressionImageService);
  private readonly messages = inject(MessagesService);

  protected readonly cameraPossible = cameraDisponible();
  protected readonly etape = signal<Etape>('choix');
  protected readonly mode = signal<Mode>('fichier');
  protected readonly preparation = signal(false);
  protected readonly survol = signal(false);
  protected readonly apercuUrl = signal<string | null>(null);
  protected readonly tailleImage = signal('');
  protected readonly progression = signal(0);
  protected readonly erreur = signal<string | null>(null);
  protected readonly resultat = signal<Scan | null>(null);
  /** Fiche complète du médicament reconnu (posologie, contre-indications...). */
  protected readonly ficheMedicament = signal<Medicament | null>(null);

  private image: Blob | null = null;

  constructor() {
    // Libère la mémoire de l'aperçu en quittant la page
    inject(DestroyRef).onDestroy(() => this.libererApercu());
  }

  // ---------- 1. Choix de l'image ----------

  /** Fichier choisi via un bouton (ou l'appareil photo du téléphone). */
  surSelectionFichier(evenement: Event): void {
    const champ = evenement.target as HTMLInputElement;
    const fichier = champ.files?.[0];
    champ.value = ''; // permet de rechoisir le même fichier
    if (fichier) {
      void this.preparerFichier(fichier);
    }
  }

  surSurvol(evenement: DragEvent, actif: boolean): void {
    evenement.preventDefault(); // obligatoire pour autoriser le dépôt
    this.survol.set(actif);
  }

  surDepot(evenement: DragEvent): void {
    evenement.preventDefault();
    this.survol.set(false);
    const fichier = evenement.dataTransfer?.files?.[0];
    if (fichier) {
      void this.preparerFichier(fichier);
    }
  }

  /** Photo prise avec la webcam. */
  surPhotoWebcam(photo: Blob): void {
    void this.preparerImage(photo);
  }

  async preparerFichier(fichier: File): Promise<void> {
    const probleme = verifierFichierImage(fichier);
    if (probleme) {
      this.messages.erreur(probleme);
      return;
    }
    await this.preparerImage(fichier);
  }

  /** Compresse l'image puis affiche l'aperçu. */
  private async preparerImage(source: Blob): Promise<void> {
    this.preparation.set(true);
    this.erreur.set(null);
    try {
      const compressee = await this.compression.compresser(source);
      this.libererApercu();
      this.image = compressee;
      this.apercuUrl.set(URL.createObjectURL(compressee));
      this.tailleImage.set(formaterTaille(compressee.size));
      this.etape.set('apercu');
    } catch {
      this.messages.erreur('Impossible de lire cette image. Essayez avec une photo JPEG ou PNG.');
    } finally {
      this.preparation.set(false);
    }
  }

  // ---------- 2-3. Envoi ----------

  envoyer(): void {
    if (!this.image) {
      return;
    }
    this.etape.set('envoi');
    this.progression.set(0);
    this.erreur.set(null);

    this.scanService.envoyer(this.image).subscribe({
      next: (evenement) => {
        if (evenement.type === HttpEventType.UploadProgress && evenement.total) {
          this.progression.set(Math.round((100 * evenement.loaded) / evenement.total));
        } else if (evenement.type === HttpEventType.Response && evenement.body) {
          this.afficherResultat(evenement.body);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.erreur.set(
          err.status === 413
            ? 'L\'image est trop volumineuse pour le serveur.'
            : messageErreurApi(err, 'L\'envoi a échoué. Vérifiez votre connexion puis réessayez.'),
        );
        this.etape.set('apercu'); // la photo est conservée : l'utilisateur peut réessayer
      },
    });
  }

  // ---------- 4. Résultat ----------

  private afficherResultat(scan: Scan): void {
    this.resultat.set(scan);
    this.ficheMedicament.set(null);
    this.etape.set('termine');
    // Médicament reconnu : on charge sa fiche complète (posologie, contre-indications, interactions)
    if (scan.medicament) {
      this.medicamentService.getMedicament(scan.medicament.id).subscribe({
        next: (fiche) => this.ficheMedicament.set(fiche),
        error: () => this.ficheMedicament.set(null), // le résumé du médicament reste affiché
      });
    }
  }

  // ---------- 5. Recommencer ----------

  recommencer(): void {
    this.libererApercu();
    this.image = null;
    this.resultat.set(null);
    this.ficheMedicament.set(null);
    this.erreur.set(null);
    this.etape.set('choix');
  }

  private libererApercu(): void {
    const url = this.apercuUrl();
    if (url) {
      URL.revokeObjectURL(url);
      this.apercuUrl.set(null);
    }
  }
}