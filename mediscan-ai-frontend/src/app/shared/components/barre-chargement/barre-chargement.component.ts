import { Component, inject } from '@angular/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { ChargementService } from '../../../core/services/chargement.service';

/** Fine barre en haut de l'écran, visible pendant les appels à l'API. */
@Component({
  selector: 'app-barre-chargement',
  imports: [MatProgressBarModule],
  template: `
    @if (chargement.actif()) {
      <mat-progress-bar mode="indeterminate" class="barre" aria-label="Chargement en cours" />
    }
  `,
  styles: `
    .barre {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      z-index: 1100;
      height: 3px;
    }
  `,
})
export class BarreChargementComponent {
  protected readonly chargement = inject(ChargementService);
}