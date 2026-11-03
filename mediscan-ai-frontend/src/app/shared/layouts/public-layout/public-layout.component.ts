import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterOutlet } from '@angular/router';

import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';

/** Mise en page des pages publiques : accueil, connexion, inscription. */
@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, RouterLink, MatToolbarModule, MatIconModule, MatButtonModule],
  templateUrl: './public-layout.component.html',
  styleUrl: './public-layout.component.scss',
})
export class PublicLayoutComponent {
  protected readonly auth = inject(AuthService);
  protected readonly nomApplication = environment.nomApplication;
}