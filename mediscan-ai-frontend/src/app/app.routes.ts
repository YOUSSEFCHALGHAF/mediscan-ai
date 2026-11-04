import { Routes } from '@angular/router';

import { authGuard, inviteGuard, roleGuard } from './core/guards/auth.guard';
import { PublicLayoutComponent } from './shared/layouts/public-layout/public-layout.component';

/**
 * Routes principales :
 * - pages publiques (accueil, connexion, inscriptions) dans la mise en page publique ;
 * - /patient : espace patient (connecté + rôle PATIENT) ;
 * - /medecin : espace médecin (connecté + rôle MEDECIN).
 */
export const routes: Routes = [
  {
    path: '',
    component: PublicLayoutComponent,
    children: [
      {
        path: '',
        title: 'MediScan AI',
        loadComponent: () => import('./accueil/accueil.component').then((m) => m.AccueilComponent),
      },
      {
        path: 'connexion',
        title: 'Connexion - MediScan AI',
        canActivate: [inviteGuard],
        loadComponent: () =>
          import('./auth/connexion/connexion.component').then((m) => m.ConnexionComponent),
      },
      {
        path: 'inscription',
        title: 'Inscription - MediScan AI',
        canActivate: [inviteGuard],
        loadComponent: () =>
          import('./auth/inscription-patient/inscription-patient.component').then(
            (m) => m.InscriptionPatientComponent,
          ),
      },
      {
        path: 'inscription/medecin',
        title: 'Inscription médecin - MediScan AI',
        canActivate: [inviteGuard],
        loadComponent: () =>
          import('./auth/inscription-medecin/inscription-medecin.component').then(
            (m) => m.InscriptionMedecinComponent,
          ),
      },
    ],
  },
  {
    path: 'patient',
    canActivate: [authGuard, roleGuard('PATIENT')],
    loadChildren: () => import('./patient/patient.routes').then((m) => m.PATIENT_ROUTES),
  },
  {
    path: 'medecin',
    canActivate: [authGuard, roleGuard('MEDECIN')],
    loadChildren: () => import('./medecin/medecin.routes').then((m) => m.MEDECIN_ROUTES),
  },
  { path: '**', redirectTo: '' },
];