/**
 * Configuration de PRODUCTION (utilisée par `ng build`).
 * En production, Angular et l'API sont servis par le même serveur (Nginx, étape 22) :
 * l'URL relative "/api" évite tout problème CORS.
 */
export const environment = {
  production: true,
  nomApplication: 'MediScan AI',
  apiUrl: '/api',
  firebase: {
    apiKey: '',
    authDomain: '',
    projectId: '',
    storageBucket: '',
    messagingSenderId: '',
    appId: '',
    vapidKey: '',
  },
};