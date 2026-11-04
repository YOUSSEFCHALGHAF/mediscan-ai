import { AfterViewInit, Component, DestroyRef, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { messageErreurCamera } from '../../../core/utils/camera';

/**
 * Capture d'une photo avec la caméra de l'appareil (webcam ou caméra arrière du téléphone).
 * Utilise l'API navigateur getUserMedia. La caméra est coupée dès que le composant disparaît.
 */
@Component({
  selector: 'app-capture-webcam',
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './capture-webcam.component.html',
  styleUrl: './capture-webcam.component.scss',
})
export class CaptureWebcamComponent implements AfterViewInit {
  /** Émis avec la photo prise (JPEG). */
  readonly photoPrise = output<Blob>();

  private readonly video = viewChild.required<ElementRef<HTMLVideoElement>>('video');
  private flux: MediaStream | null = null;

  protected readonly etat = signal<'demarrage' | 'active' | 'erreur'>('demarrage');
  protected readonly erreur = signal<string | null>(null);
  protected readonly cameras = signal<MediaDeviceInfo[]>([]);
  private indexCamera = 0;

  constructor() {
    // Indispensable : éteindre la caméra (et son voyant) en quittant la page
    inject(DestroyRef).onDestroy(() => this.arreter());
  }

  ngAfterViewInit(): void {
    void this.demarrer();
  }

  /** Démarre la caméra : caméra arrière ("environment") de préférence sur téléphone. */
  async demarrer(deviceId?: string): Promise<void> {
    this.arreter();
    this.etat.set('demarrage');
    this.erreur.set(null);

    if (!navigator.mediaDevices?.getUserMedia) {
      this.echec(new DOMException('Contexte non sécurisé', 'SecurityError'));
      return;
    }

    try {
      this.flux = await navigator.mediaDevices.getUserMedia({
        audio: false,
        video: deviceId
          ? { deviceId: { exact: deviceId } }
          : { facingMode: { ideal: 'environment' }, width: { ideal: 1920 }, height: { ideal: 1080 } },
      });
      const video = this.video().nativeElement;
      video.srcObject = this.flux;
      await video.play();
      this.etat.set('active');

      // La liste des caméras n'est complète qu'après l'autorisation de l'utilisateur
      const appareils = await navigator.mediaDevices.enumerateDevices();
      this.cameras.set(appareils.filter((a) => a.kind === 'videoinput'));
    } catch (e) {
      this.echec(e);
    }
  }

  /** Passe à la caméra suivante (avant / arrière, webcam externe...). */
  changerCamera(): void {
    const liste = this.cameras();
    if (liste.length < 2) {
      return;
    }
    this.indexCamera = (this.indexCamera + 1) % liste.length;
    void this.demarrer(liste[this.indexCamera].deviceId);
  }

  /** Copie l'image affichée par la vidéo dans un canvas, puis la convertit en JPEG. */
  capturer(): void {
    const video = this.video().nativeElement;
    if (!video.videoWidth) {
      return;
    }
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d')?.drawImage(video, 0, 0);
    canvas.toBlob((blob) => blob && this.photoPrise.emit(blob), 'image/jpeg', 0.92);
  }

  private arreter(): void {
    this.flux?.getTracks().forEach((piste) => piste.stop());
    this.flux = null;
  }

  private echec(e: unknown): void {
    this.arreter();
    this.erreur.set(messageErreurCamera(e));
    this.etat.set('erreur');
  }
}