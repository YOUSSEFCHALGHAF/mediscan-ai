import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, computed, inject, input, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';

/** Élément du menu latéral. */
export interface EntreeMenu {
  lien: string;
  libelle: string;
  icone: string;
}

/**
 * Mise en page COMMUNE aux espaces patient et médecin :
 * - ordinateur/tablette : menu latéral toujours visible ;
 * - smartphone (< 960 px) : menu en tiroir, ouvert avec le bouton ☰.
 * Le titre et le menu sont fournis par la route (data), grâce à withComponentInputBinding.
 */
@Component({
  selector: 'app-espace-layout',
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatSidenavModule, MatToolbarModule, MatListModule, MatIconModule, MatButtonModule, MatMenuModule, MatDividerModule,
  ],
  templateUrl: './espace-layout.component.html',
  styleUrl: './espace-layout.component.scss',
})
export class EspaceLayoutComponent {
  protected readonly auth = inject(AuthService);
  private readonly sidenav = viewChild.required<MatSidenav>('sidenav');

  /** Données de la route */
  readonly titreEspace = input('Mon espace');
  readonly menu = input<EntreeMenu[]>([]);
  /** Lien vers la page profil (null = pas d'entrée "Mon profil" dans le menu du compte). */
  readonly lienProfil = input<string | null>(null);

  protected readonly estMobile = toSignal(
    inject(BreakpointObserver).observe('(max-width: 959px)').pipe(map((r) => r.matches)),
    { initialValue: false },
  );

  protected readonly initiales = computed(() => {
    const u = this.auth.utilisateur();
    return u ? `${u.prenom.charAt(0)}${u.nom.charAt(0)}`.toUpperCase() : '';
  });

  /** "Dr Karim Mansour" pour un médecin, "Marc Lefèvre" pour un patient. */
  protected readonly nomAffiche = computed(() =>
    this.auth.role() === 'MEDECIN' ? `Dr ${this.auth.nomComplet()}` : this.auth.nomComplet(),
  );

  fermerSiMobile(): void {
    if (this.estMobile()) {
      this.sidenav().close();
    }
  }

  seDeconnecter(): void {
    this.auth.deconnecter();
  }
}