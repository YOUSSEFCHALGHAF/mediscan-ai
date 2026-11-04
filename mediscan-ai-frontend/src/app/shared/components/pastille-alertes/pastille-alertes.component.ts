import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/** Pastille colorée indiquant le nombre d'alertes non traitées (vert si 0, rouge sinon). */
@Component({
  selector: 'app-pastille-alertes',
  imports: [MatIconModule],
  template: `
    <span class="pastille" [class.alerte]="nombre() > 0" [attr.aria-label]="nombre() + ' alerte(s) non traitée(s)'">
      <mat-icon>{{ nombre() > 0 ? 'warning' : 'check' }}</mat-icon>
      {{ nombre() }}
    </span>
  `,
  styles: `
    .pastille {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 2px 10px; border-radius: 999px;
      font: var(--mat-sys-label-large);
      background: color-mix(in srgb, var(--ms-succes) 15%, transparent);
      color: var(--ms-succes);
    }
    .pastille.alerte {
      background: var(--mat-sys-error-container);
      color: var(--mat-sys-on-error-container);
    }
    mat-icon { font-size: 18px; width: 18px; height: 18px; }
  `,
})
export class PastilleAlertesComponent {
  readonly nombre = input(0);
}