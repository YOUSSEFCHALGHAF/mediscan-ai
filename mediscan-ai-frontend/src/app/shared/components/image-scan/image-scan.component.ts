import { Component, effect, inject, input, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import { ScanService } from '../../../core/services/scan.service';

/**
 * Affiche la photo d'un scan. L'image est protégée par le token JWT :
 * une balise <img src="/api/..."> ne l'enverrait pas. On télécharge donc l'image
 * avec HttpClient (l'intercepteur ajoute le token), puis on l'affiche via une URL locale "blob:".
 */
@Component({
  selector: 'app-image-scan',
  imports: [MatIconModule],
  template: `
    @switch (etat()) {
      @case ('ok') {
        <img [src]="url()" [alt]="alt()" />
      }
      @case ('erreur') {
        <div class="substitut" role="img" [attr.aria-label]="alt() + ' (photo indisponible)'">
          <mat-icon>image_not_supported</mat-icon>
        </div>
      }
      @default {
        <div class="substitut chargement" aria-hidden="true"><mat-icon>image</mat-icon></div>
      }
    }
  `,
  styles: `
    :host {
      display: block;
      overflow: hidden;
      background: var(--mat-sys-surface-container-high);
    }
    img {
      display: block;
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .substitut {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 100%;
      min-height: 64px;
      color: var(--mat-sys-on-surface-variant);
    }
    .chargement {
      animation: pulsation 1.2s ease-in-out infinite;
    }
    @keyframes pulsation {
      50% { opacity: 0.4; }
    }
  `,
})
export class ImageScanComponent {
  private readonly scanService = inject(ScanService);

  readonly scanId = input.required<number>();
  readonly alt = input('Photo du médicament');

  protected readonly url = signal<string | null>(null);
  protected readonly etat = signal<'chargement' | 'ok' | 'erreur'>('chargement');

  constructor() {
    // Se relance si scanId change ; onCleanup annule la requête et libère la mémoire de l'image
    effect((onCleanup) => {
      const id = this.scanId();
      this.etat.set('chargement');
      let urlLocale: string | null = null;

      const abonnement = this.scanService.getImage(id).subscribe({
        next: (image) => {
          urlLocale = URL.createObjectURL(image);
          this.url.set(urlLocale);
          this.etat.set('ok');
        },
        error: () => this.etat.set('erreur'), // ex. photos des données de démonstration absentes du disque
      });

      onCleanup(() => {
        abonnement.unsubscribe();
        if (urlLocale) {
          URL.revokeObjectURL(urlLocale);
        }
      });
    });
  }
}