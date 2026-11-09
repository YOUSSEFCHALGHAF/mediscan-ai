import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { NotificationPushService } from '../../../core/notifications/notification-push.service';
import { AuthService } from '../../../core/services/auth.service';

/** Clé de stockage : date jusqu'à laquelle l'utilisateur a choisi "Plus tard". */
const CLE_PLUS_TARD = 'mediscan.notifications.plusTard';
const SEPT_JOURS_MS = 7 * 24 * 60 * 60 * 1000;

/**
 * Invitation à activer les notifications, affichée APRÈS la connexion, avec une explication.
 * Bonne pratique : la fenêtre d'autorisation du navigateur ne s'ouvre qu'au clic sur "Activer"
 * (une demande au chargement de la page est souvent refusée par réflexe, et ce refus est définitif).
 */
@Component({
  selector: 'app-bandeau-notifications',
  imports: [MatButtonModule, MatIconModule],
  template: `
    @if (visible()) {
      <aside class="bandeau" role="region" aria-label="Activer les notifications">
        <mat-icon class="icone">notifications_active</mat-icon>
        <p>
          <strong>Activez les notifications</strong><br />
          {{ explication() }}
        </p>
        <div class="actions">
          <button mat-button type="button" (click)="plusTard()">Plus tard</button>
          <button mat-flat-button type="button" [disabled]="enCours()" (click)="activer()">Activer</button>
        </div>
      </aside>
    }
  `,
  styles: `
    .bandeau {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 12px 16px;
      margin-bottom: 16px;
      padding: 12px 16px;
      border-radius: 12px;
      background: var(--mat-sys-primary-container);
      color: var(--mat-sys-on-primary-container);
    }
    .icone {
      flex-shrink: 0;
    }
    p {
      flex: 1;
      min-width: 220px;
      margin: 0;
    }
    .actions {
      display: flex;
      gap: 8px;
      margin-left: auto;
    }
  `,
})
export class BandeauNotificationsComponent {
  private readonly push = inject(NotificationPushService);
  private readonly auth = inject(AuthService);

  private readonly masque = signal(this.lirePlusTard() > Date.now());
  protected readonly enCours = signal(false);

  protected readonly visible = computed(() => this.push.etat() === 'a-demander' && !this.masque());

  protected readonly explication = computed(() =>
    this.auth.role() === 'MEDECIN'
      ? 'Soyez prévenu immédiatement lorsqu\'une alerte grave concerne l\'un de vos patients.'
      : 'Soyez prévenu immédiatement en cas d\'interaction dangereuse, et à l\'heure de vos prises de médicaments.',
  );

  protected async activer(): Promise<void> {
    this.enCours.set(true);
    await this.push.activer();
    this.enCours.set(false);
  }

  protected plusTard(): void {
    try {
      localStorage.setItem(CLE_PLUS_TARD, String(Date.now() + SEPT_JOURS_MS));
    } catch {
      // stockage indisponible (navigation privée) : on masque seulement pour cette visite
    }
    this.masque.set(true);
  }

  private lirePlusTard(): number {
    try {
      return Number(localStorage.getItem(CLE_PLUS_TARD)) || 0;
    } catch {
      return 0;
    }
  }
}