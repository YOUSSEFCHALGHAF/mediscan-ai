import { Routes } from '@angular/router';

import { PatientLayoutComponent } from './layout/patient-layout/patient-layout.component';
/**
 * Routes de l'espace patient (/patient/...).
 * Chargées uniquement quand un patient se connecte (lazy loading).
 * Les rubriques encore en construction affichent une page "à venir".
 */
const pageAVenir = () =>
  import('../shared/components/page-a-venir/page-a-venir.component').then((m) => m.PageAVenirComponent);

export const PATIENT_ROUTES: Routes = [
  {
    path: '',
    component: PatientLayoutComponent,
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
        loadComponent: pageAVenir,
        data: { titre: 'Mon anamnèse', etape: 12, icone: 'assignment' },
      },
      {
        path: 'scanner',
        title: 'Scanner un médicament - MediScan AI',
        loadComponent: pageAVenir,
        data: { titre: 'Scanner un médicament', etape: 13, icone: 'photo_camera' },
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