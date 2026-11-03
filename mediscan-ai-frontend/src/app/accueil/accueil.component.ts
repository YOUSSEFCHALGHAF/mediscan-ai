import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import { EtatApiComponent } from '../shared/components/etat-api/etat-api.component';

/**
 * Page d'accueil publique (temporaire à l'étape 8).
 * Elle deviendra la page de présentation avec les boutons Connexion / Inscription à l'étape 9.
 */
@Component({
  selector: 'app-accueil',
  imports: [MatIconModule, EtatApiComponent],
  templateUrl: './accueil.component.html',
  styleUrl: './accueil.component.scss',
})
export class AccueilComponent {}