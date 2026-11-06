import { Routes } from '@angular/router';

import { EntreeMenu, EspaceLayoutComponent } from '../shared/layouts/espace-layout/espace-layout.component';

/** Menu latéral de l'espace médecin. */
const MENU_MEDECIN: EntreeMenu[] = [
  { lien: 'tableau-de-bord', libelle: 'Tableau de bord', icone: 'dashboard' },
  { lien: 'patients', libelle: 'Mes patients', icone: 'groups' },
  { lien: 'alertes', libelle: 'Alertes', icone: 'warning' },
];

/**
 * Routes de l'espace médecin (/medecin/...), chargées uniquement à la connexion d'un médecin.
 */
export const MEDECIN_ROUTES: Routes = [
  {
    path: '',
    component: EspaceLayoutComponent,
    data: { titreEspace: 'Espace médecin', menu: MENU_MEDECIN },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'tableau-de-bord' },
      {
        path: 'tableau-de-bord',
        title: 'Tableau de bord médecin - MediScan AI',
        loadComponent: () =>
          import('./tableau-de-bord/tableau-de-bord-medecin.component').then((m) => m.TableauDeBordMedecinComponent),
      },
      {
        path: 'patients',
        title: 'Mes patients - MediScan AI',
        loadComponent: () => import('./mes-patients/mes-patients.component').then((m) => m.MesPatientsComponent),
      },
      {
        path: 'patients/:patientId',
        title: 'Fiche patient - MediScan AI',
        loadComponent: () =>
          import('./fiche-patient/fiche-patient.component').then((m) => m.FichePatientComponent),
      },
            {
        path: 'alertes',
        loadComponent: () =>
          import('../shared/components/page-a-venir/page-a-venir.component').then((m) => m.PageAVenirComponent),
        data: { titre: 'Alertes de mes patients', etape: 16 },
        title: 'Alertes - MediScan AI',
      },
    ],
  },
];