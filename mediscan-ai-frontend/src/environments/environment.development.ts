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
    apiKey: "AIzaSyBaSnowZmHsOQ4H9ucvdA_ZKzE3ssYSrjg",
    authDomain: "medicascan.firebaseapp.com",
    projectId: "medicascan",
    storageBucket: "medicascan.firebasestorage.app",
    messagingSenderId: "349775580325",
    appId: "1:349775580325:web:c4a64960970ac8c129914c",
    vapidKey: 'BFwWIvr-NH1Kggkr2nL7OBiFsN5Qj7-2m7EDOVZEb7vkhTdKrzKsJaBYkF1YlTbwbwmDuGerq2fj263Tl_x9KkA',
  },
};