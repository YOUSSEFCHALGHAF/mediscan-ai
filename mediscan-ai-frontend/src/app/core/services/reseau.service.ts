import { DestroyRef, inject, Service, signal } from '@angular/core';

import { MessagesService } from './messages.service';

/**
 * Suit l'état de la connexion Internet du navigateur (événements "online" / "offline").
 */
@Service()
export class ReseauService {
  private readonly messages = inject(MessagesService);

  readonly enLigne = signal(navigator.onLine);

  constructor() {
    const surEnLigne = () => {
      this.enLigne.set(true);
      this.messages.succes('Connexion Internet rétablie');
    };
    const surHorsLigne = () => this.enLigne.set(false);

    window.addEventListener('online', surEnLigne);
    window.addEventListener('offline', surHorsLigne);
    inject(DestroyRef).onDestroy(() => {
      window.removeEventListener('online', surEnLigne);
      window.removeEventListener('offline', surHorsLigne);
    });
  }
}