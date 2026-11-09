import { InjectionToken } from '@angular/core';

import { environment } from '../../../environments/environment';

/** Configuration Firebase de l'application Web (console Firebase → Paramètres du projet). */
export interface ConfigFirebase {
  apiKey: string;
  authDomain: string;
  projectId: string;
  storageBucket: string;
  messagingSenderId: string;
  appId: string;
  /** Clé publique "Web Push" (console Firebase → Cloud Messaging → Certificats Web Push). */
  vapidKey: string;
}

/** Fournie par défaut depuis environment.ts ; remplaçable dans les tests. */
export const CONFIG_FIREBASE = new InjectionToken<ConfigFirebase>('CONFIG_FIREBASE', {
  providedIn: 'root',
  factory: () => environment.firebase,
});

/** Vrai si la configuration a été renseignée (sinon, les notifications sont simplement désactivées). */
export function firebaseConfigure(config: ConfigFirebase): boolean {
  return !!config.apiKey && !!config.projectId && !!config.messagingSenderId && !!config.appId && !!config.vapidKey;
}