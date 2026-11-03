import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

/**
 * Page provisoire pour les rubriques qui seront développées aux étapes suivantes.
 * Les valeurs viennent du "data" de la route (grâce à withComponentInputBinding).
 */
@Component({
  selector: 'app-page-a-venir',
  imports: [MatCardModule, MatIconModule],
  template: `
    <h1 class="titre-page">{{ titre() }}</h1>
    <mat-card appearance="outlined">
      <mat-card-content class="contenu">
        <mat-icon class="icone">{{ icone() }}</mat-icon>
        <p>Cette rubrique sera développée à l'<strong>étape {{ etape() }}</strong> du projet.</p>
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    .contenu { display: flex; align-items: center; gap: 16px; padding: 16px 0; }
    .icone { font-size: 40px; width: 40px; height: 40px; color: var(--mat-sys-primary); }
  `,
})
export class PageAVenirComponent {
  readonly titre = input('Rubrique');
  readonly etape = input<number>();
  readonly icone = input('construction');
}