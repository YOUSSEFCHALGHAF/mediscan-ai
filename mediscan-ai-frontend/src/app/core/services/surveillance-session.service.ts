import { DestroyRef, effect, inject, Service, untracked } from '@angular/core';

import { AuthService } from './auth.service';
import { MessagesService } from './messages.service';

/** L'avertissement apparaît 5 minutes avant l'expiration du token. */
export const DELAI_AVERTISSEMENT_MS = 5 * 60 * 1000;

/**
 * Surveille l'expiration de la session :
 * - avertit l'utilisateur 5 minutes avant la fin ;
 * - le déconnecte proprement à l'expiration (au lieu d'attendre une erreur 401).
 * Se reprogramme automatiquement à chaque connexion / déconnexion.
 */
@Service()
export class SurveillanceSessionService {
  private readonly auth = inject(AuthService);
  private readonly messages = inject(MessagesService);
  private minuteurs: ReturnType<typeof setTimeout>[] = [];

  constructor() {
    // effect : réexécuté dès que la date d'expiration change (connexion, déconnexion)
    effect(() => {
      const expiration = this.auth.expiration();
      untracked(() => this.planifier(expiration));
    });
    inject(DestroyRef).onDestroy(() => this.annuler());
  }

  private planifier(expiration: number | null): void {
    this.annuler();
    if (!expiration) {
      return;
    }
    const restant = expiration - Date.now();
    if (restant <= 0) {
      this.auth.deconnecter('expiree');
      return;
    }

    const avantAvertissement = restant - DELAI_AVERTISSEMENT_MS;
    if (avantAvertissement > 0) {
      this.minuteurs.push(setTimeout(() => this.avertir(), avantAvertissement));
    } else {
      this.avertir();
    }
    this.minuteurs.push(setTimeout(() => this.auth.deconnecter('expiree'), restant));
  }

  private avertir(): void {
    const expiration = this.auth.expiration();
    if (!expiration) {
      return;
    }
    const minutes = Math.max(1, Math.round((expiration - Date.now()) / 60000));
    this.messages.avertissement(
      `Votre session expire dans ${minutes} minute(s). Enregistrez vos modifications, puis reconnectez-vous.`,
      15000,
    );
  }

  private annuler(): void {
    this.minuteurs.forEach((m) => clearTimeout(m));
    this.minuteurs = [];
  }
}