import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { environment } from '../../../../environments/environment';
import { EtatSante } from '../../../core/models';
import { SanteService } from '../../../core/services/sante.service';

/**
 * Carte réutilisable affichant l'état de l'API et de la base MySQL.
 * Premier test réel de la connexion Angular -> Spring Boot (et donc de CORS).
 */
@Component({
  selector: 'app-etat-api',
  imports: [DatePipe, MatCardModule, MatIconModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './etat-api.component.html',
  styleUrl: './etat-api.component.scss',
})
export class EtatApiComponent implements OnInit {
  private readonly santeService = inject(SanteService);

  protected readonly apiUrl = environment.apiUrl;
  protected readonly chargement = signal(false);
  protected readonly etat = signal<EtatSante | null>(null);
  protected readonly erreur = signal<string | null>(null);

  ngOnInit(): void {
    this.verifier();
  }

  verifier(): void {
    this.chargement.set(true);
    this.erreur.set(null);

    this.santeService.verifier().subscribe({
      next: (reponse) => {
        this.etat.set(reponse);
        this.chargement.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.etat.set(null);
        this.erreur.set(this.expliquer(err));
        this.chargement.set(false);
      },
    });
  }

  /** Traduit l'erreur HTTP en message compréhensible. */
  private expliquer(err: HttpErrorResponse): string {
    if (err.status === 0) {
      // Status 0 = le navigateur n'a reçu AUCUNE réponse exploitable
      return 'Impossible de joindre l\'API : le backend est arrêté, ou la requête est bloquée par CORS '
        + '(ouvrez la console du navigateur avec F12 pour voir le détail).';
    }
    return `L'API a répondu avec une erreur ${err.status} (${err.statusText}).`;
  }
}