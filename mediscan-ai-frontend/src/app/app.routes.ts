import { Routes } from '@angular/router';

/**
 * Routes de l'application.
 * loadComponent = chargement "paresseux" : le code d'une page n'est téléchargé
 * que lorsque l'utilisateur la visite (application plus rapide au démarrage).
 * Les espaces /auth, /patient et /medecin seront ajoutés aux étapes 9 à 11.
 */
export const routes: Routes = [
  {
    path: '',
    title: 'MediScan AI',
    loadComponent: () =>
      import('./accueil/accueil.component').then((m) => m.AccueilComponent),
  },
  { path: '**', redirectTo: '' },
];