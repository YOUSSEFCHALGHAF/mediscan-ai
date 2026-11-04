import { computed, Service, signal } from '@angular/core';

/**
 * Compte les requêtes HTTP en cours.
 * La barre de chargement globale est visible tant qu'au moins une requête est active.
 */
@Service()
export class ChargementService {
  private readonly requetesEnCours = signal(0);

  readonly actif = computed(() => this.requetesEnCours() > 0);

  debut(): void {
    this.requetesEnCours.update((n) => n + 1);
  }

  fin(): void {
    this.requetesEnCours.update((n) => Math.max(0, n - 1));
  }
}