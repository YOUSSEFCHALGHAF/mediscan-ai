import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { AuthService } from '../core/services/auth.service';
import { EtatApiComponent } from '../shared/components/etat-api/etat-api.component';

/** Page d'accueil publique : présentation + accès connexion / inscription. */
@Component({
  selector: 'app-accueil',
  imports: [RouterLink, MatButtonModule, MatCardModule, MatIconModule, EtatApiComponent],
  templateUrl: './accueil.component.html',
  styleUrl: './accueil.component.scss',
})
export class AccueilComponent {
  protected readonly auth = inject(AuthService);

  protected readonly fonctionnalites = [
    {
      icone: 'assignment',
      titre: 'Anamnèse guidée',
      texte: 'Renseignez vos antécédents, allergies et traitements en quelques étapes simples.',
    },
    {
      icone: 'photo_camera',
      titre: 'Scan de médicaments',
      texte: 'Photographiez une boîte : l\'IA reconnaît le médicament, son dosage et sa date d\'expiration.',
    },
    {
      icone: 'verified_user',
      titre: 'Partage sécurisé',
      texte: 'Partagez vos données avec votre médecin, uniquement avec votre consentement, et révoquez-le à tout moment.',
    },
  ];
}