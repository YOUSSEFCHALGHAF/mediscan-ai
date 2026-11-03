import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, computed, inject, viewChild } from '@angular/core';
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
interface EntreeMenu {
  lien: string;
  libelle: string;
  icone: string;
}

/**
 * Mise en page de l'espace patient :
 * - ordinateur/tablette : menu latéral toujours visible ;
 * - smartphone (< 960 px) : menu en tiroir, ouvert avec le bouton ☰.
 */
@Component({
  selector: 'app-patient-layout',
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatSidenavModule, MatToolbarModule, MatListModule, MatIconModule, MatButtonModule, MatMenuModule, MatDividerModule,
  ],
  templateUrl: './patient-layout.component.html',
  styleUrl: './patient-layout.component.scss',
})
export class PatientLayoutComponent {
  protected readonly auth = inject(AuthService);
  private readonly sidenav = viewChild.required<MatSidenav>('sidenav');

  /** true sur petit écran (signal mis à jour automatiquement quand la fenêtre change de taille). */
  protected readonly estMobile = toSignal(
    inject(BreakpointObserver).observe('(max-width: 959px)').pipe(map((r) => r.matches)),
    { initialValue: false },
  );

  protected readonly initiales = computed(() => {
    const u = this.auth.utilisateur();
    return u ? `${u.prenom.charAt(0)}${u.nom.charAt(0)}`.toUpperCase() : '';
  });

  protected readonly menu: EntreeMenu[] = [
    { lien: 'tableau-de-bord', libelle: 'Tableau de bord', icone: 'dashboard' },
    { lien: 'profil', libelle: 'Mon profil', icone: 'person' },
    { lien: 'anamnese', libelle: 'Mon anamnèse', icone: 'assignment' },
    { lien: 'scanner', libelle: 'Scanner un médicament', icone: 'photo_camera' },
    { lien: 'historique', libelle: 'Historique', icone: 'history' },
    { lien: 'rappels', libelle: 'Rappels', icone: 'alarm' },
    { lien: 'partage', libelle: 'Partage médecin', icone: 'share' },
  ];

  /** Sur mobile, referme le tiroir après un clic dans le menu. */
  fermerSiMobile(): void {
    if (this.estMobile()) {
      this.sidenav().close();
    }
  }

  seDeconnecter(): void {
    this.auth.deconnecter();
  }
}