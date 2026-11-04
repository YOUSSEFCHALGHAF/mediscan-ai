import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import { ReseauService } from '../../../core/services/reseau.service';

/** Bandeau affiché tant que le navigateur n'a plus de connexion Internet. */
@Component({
  selector: 'app-bandeau-hors-ligne',
  imports: [MatIconModule],
  template: `
    @if (!reseau.enLigne()) {
      <div class="bandeau" role="status">
        <mat-icon>wifi_off</mat-icon>
        <span>Vous êtes hors ligne. Les données affichées peuvent ne pas être à jour.</span>
      </div>
    }
  `,
  styles: `
    .bandeau {
      position: fixed;
      bottom: 0;
      left: 0;
      right: 0;
      z-index: 1100;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 10px 16px;
      background: var(--mat-sys-inverse-surface);
      color: var(--mat-sys-inverse-on-surface);
      font: var(--mat-sys-body-medium);
    }
  `,
})
export class BandeauHorsLigneComponent {
  protected readonly reseau = inject(ReseauService);
}