/**
 * Configuration de développement AVEC PROXY (`ng serve --configuration proxy`).
 * Le navigateur appelle "/api/..." sur le port 4200 ; le serveur de développement Angular
 * transmet la requête à http://localhost:8080 (voir proxy.conf.json).
 * Le navigateur ne voit qu'une seule origine : aucune vérification CORS n'a lieu.
 */
export const environment = {
  production: false,
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