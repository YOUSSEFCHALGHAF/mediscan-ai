import { Service } from '@angular/core';
import type { Messaging } from 'firebase/messaging';

import { ConfigFirebase } from './config-firebase';

/** Notification reçue alors que l'application est ouverte. */
export interface MessageRecu {
  titre: string;
  corps: string;
  type: string | null;
  lien: string | null;
}

/**
 * Accès au SDK Firebase, isolé dans cette classe :
 * - le SDK n'est téléchargé qu'au premier usage (import dynamique) : le chargement initial reste léger ;
 * - les tests remplacent cette classe par une version simulée.
 */
@Service()
export class FirebaseClient {
  private messaging: Messaging | null = null;

  /** Le navigateur permet-il le Web Push ? (HTTPS ou localhost, service worker, API Push) */
  async estSupporte(): Promise<boolean> {
    if (typeof window === 'undefined' || !('Notification' in window) || !('serviceWorker' in navigator) || !window.isSecureContext) {
      return false;
    }
    const { isSupported } = await import('firebase/messaging');
    return isSupported();
  }

  /** Enregistre le service worker et renvoie le token FCM de ce navigateur. */
  async obtenirToken(config: ConfigFirebase): Promise<string> {
    const { getApps, initializeApp } = await import('firebase/app');
    const { getMessaging, getToken } = await import('firebase/messaging');
    const { vapidKey, ...configApplication } = config;

    const app = getApps()[0] ?? initializeApp(configApplication);
    const enregistrement = await navigator.serviceWorker.register(
      `/firebase-messaging-sw.js?config=${encodeURIComponent(JSON.stringify(configApplication))}`,
    );
    this.messaging = getMessaging(app);
    return getToken(this.messaging, { vapidKey, serviceWorkerRegistration: enregistrement });
  }

  /** Écoute les notifications reçues quand l'application est au premier plan. Renvoie la fonction d'arrêt. */
  async ecouter(rappel: (message: MessageRecu) => void): Promise<() => void> {
    if (!this.messaging) {
      return () => undefined;
    }
    const { onMessage } = await import('firebase/messaging');
    return onMessage(this.messaging, (payload) =>
      rappel({
        titre: payload.notification?.title ?? 'MediScan AI',
        corps: payload.notification?.body ?? '',
        type: payload.data?.['type'] ?? null,
        lien: payload.data?.['lien'] ?? null,
      }),
    );
  }

  /** Invalide le token de ce navigateur (déconnexion). */
  async supprimerToken(): Promise<void> {
    if (this.messaging) {
      const { deleteToken } = await import('firebase/messaging');
      await deleteToken(this.messaging);
    }
  }
}