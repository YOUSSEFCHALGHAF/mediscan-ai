import { afterNextRender, Component, DestroyRef, effect, ElementRef, inject, input, viewChild } from '@angular/core';
import type { ChartConfiguration } from 'chart.js';

import { ChargeurGraphiques, InstanceGraphique } from '../../../core/graphiques/chargeur-graphiques';

/**
 * Affiche un graphique Chart.js. Accessible : le canvas porte un texte alternatif (role="img"),
 * car un lecteur d'écran ne peut pas "lire" un dessin.
 */
@Component({
  selector: 'app-graphique',
  template: `<canvas #canvas role="img" [attr.aria-label]="description()"></canvas>`,
  styles: `
    :host {
      display: block;
      position: relative;
      height: var(--hauteur-graphique, 260px);
    }
  `,
})
export class GraphiqueComponent {
  private readonly chargeur = inject(ChargeurGraphiques);
  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');

  readonly config = input.required<ChartConfiguration>();
  /** Résumé textuel du graphique (lecteurs d'écran). */
  readonly description = input.required<string>();

  private instance: InstanceGraphique | null = null;
  private detruit = false;

  constructor() {
    // Création une fois le canvas présent dans la page
    afterNextRender(() => {
      void this.chargeur.creer(this.canvas().nativeElement, this.config()).then((instance) => {
        if (this.detruit) {
          instance.detruire();
        } else {
          this.instance = instance;
        }
      });
    });
    // Nouvelles données : le graphique existant est mis à jour (pas recréé)
    effect(() => {
      const config = this.config();
      this.instance?.mettreAJour(config);
    });
    inject(DestroyRef).onDestroy(() => {
      this.detruit = true;
      this.instance?.detruire();
    });
  }
}