/**
 * Configuration de DÉVELOPPEMENT (utilisée par `ng serve`).
 * Angular remplace environment.ts par ce fichier (voir "fileReplacements" dans angular.json).
 */
export const environment = {
  production: false,
  nomApplication: 'MediScan AI',
  /** URL de l'API Spring Boot (doit correspondre à app.cors.allowed-origins côté backend). */
  apiUrl: 'http://192.168.3.143:8081/api',
  /** Configuration Firebase Cloud Messaging (notifications) : remplie à l'étape 17. */
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