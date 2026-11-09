import { Service } from '@angular/core';
import type { ChartConfiguration } from 'chart.js';

/** Un graphique affiché : on peut le mettre à jour ou le détruire. */
export interface InstanceGraphique {
  mettreAJour(config: ChartConfiguration): void;
  detruire(): void;
}

/**
 * Crée les graphiques Chart.js.
 * - Chart.js n'est téléchargé qu'à l'ouverture d'une page qui contient un graphique (import dynamique).
 * - Isolé dans ce service : les tests le remplacent (le "canvas" n'existe pas dans l'environnement de test).
 */
@Service()
export class ChargeurGraphiques {
  async creer(canvas: HTMLCanvasElement, config: ChartConfiguration): Promise<InstanceGraphique> {
    const { default: Chart } = await import('chart.js/auto');
    const graphique = new Chart(canvas, config);
    return {
      mettreAJour: (nouvelle) => {
        graphique.data = nouvelle.data;
        graphique.options = nouvelle.options ?? {};
        graphique.update();
      },
      detruire: () => graphique.destroy(),
    };
  }
}