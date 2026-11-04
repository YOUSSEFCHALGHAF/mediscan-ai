import { Routes } from '@angular/router';

import { modificationsNonEnregistreesGuard } from '../core/guards/modifications.guard';
import { EntreeMenu, EspaceLayoutComponent } from '../shared/layouts/espace-layout/espace-layout.component';

/** Menu latéral de l'espace patient. */
const MENU_PATIENT: EntreeMenu[] = [
  { lien: 'tableau-de-bord', libelle: 'Tableau de bord', icone: 'dashboard' },
  { lien: 'profil', libelle: 'Mon profil', icone: 'person' },
  { lien: 'anamnese', libelle: 'Mon anamnèse', icone: 'assignment' },
  { lien: 'scanner', libelle: 'Scanner un médicament', icone: 'photo_camera' },
  { lien: 'historique', libelle: 'Historique', icone: 'history' },
  { lien: 'rappels', libelle: 'Rappels', icone: 'alarm' },
  { lien: 'partage', libelle: 'Partage médecin', icone: 'share' },
];

const pageAVenir = () =>
  import('../shared/components/page-a-venir/page-a-venir.component').then((m) => m.PageAVenirComponent);

/**
 * Routes de l'espace patient (/patient/...), chargées uniquement à la connexion d'un patient.
 */
export const PATIENT_ROUTES: Routes = [
  {
    path: '',
    component: EspaceLayoutComponent,
    data: { titreEspace: 'Espace patient', menu: MENU_PATIENT, lienProfil: '/patient/profil' },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'tableau-de-bord' },
      {
        path: 'tableau-de-bord',
        title: 'Tableau de bord - MediScan AI',
        loadComponent: () =>
          import('./tableau-de-bord/tableau-de-bord.component').then((m) => m.TableauDeBordComponent),
      },
      {
        path: 'profil',
        title: 'Mon profil - MediScan AI',
        loadComponent: () => import('./profil/profil.component').then((m) => m.ProfilComponent),
      },
      {
        path: 'anamnese',
        title: 'Mon anamnèse - MediScan AI',
        loadComponent: () => import('./anamnese/anamnese.component').then((m) => m.AnamneseComponent),
        canDeactivate: [modificationsNonEnregistreesGuard],
      },
      {
        path: 'scanner',
        loadComponent: () => import('./scanner/scanner.component')
        .then(m => m.ScannerComponent),
        title: 'Scanner un médicament - MediScan AI'
        },

      {
        path: 'historique',
        title: 'Historique - MediScan AI',
        loadComponent: pageAVenir,
        data: { titre: 'Historique des scans', etape: 15, icone: 'history' },
      },
      {
        path: 'rappels',
        title: 'Rappels - MediScan AI',
        loadComponent: pageAVenir,
        data: { titre: 'Mes rappels', etape: 17, icone: 'alarm' },
      },
      {
        path: 'partage',
        title: 'Partage médecin - MediScan AI',
        loadComponent: pageAVenir,
        data: { titre: 'Partage avec un médecin', etape: 19, icone: 'share' },
      },

    ],
  },
];