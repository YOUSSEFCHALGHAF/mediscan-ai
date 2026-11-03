import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Composant racine : il affiche simplement la page courante.
 * Chaque espace (public, patient, médecin) apporte sa propre mise en page.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class App {}