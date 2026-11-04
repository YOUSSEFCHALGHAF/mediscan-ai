import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { SurveillanceSessionService } from './core/services/surveillance-session.service';
import { BandeauHorsLigneComponent } from './shared/components/bandeau-hors-ligne/bandeau-hors-ligne.component';
import { BarreChargementComponent } from './shared/components/barre-chargement/barre-chargement.component';

/**
 * Composant racine :
 * - barre de chargement globale et bandeau "hors ligne" (communs à toutes les pages) ;
 * - la page courante (router-outlet) ;
 * - démarre la surveillance de l'expiration de session.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, BarreChargementComponent, BandeauHorsLigneComponent],
  template: `
    <app-barre-chargement />
    <app-bandeau-hors-ligne />
    <router-outlet />
  `,
})
export class App {
  // L'injection suffit à démarrer le service (il vit pendant toute la durée de l'application)
  private readonly surveillanceSession = inject(SurveillanceSessionService);
}