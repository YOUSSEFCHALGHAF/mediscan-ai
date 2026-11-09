import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EtatNotifications, NotificationPushService } from '../../../core/notifications/notification-push.service';
import { AuthService } from '../../../core/services/auth.service';
import { BandeauNotificationsComponent } from './bandeau-notifications.component';

describe('BandeauNotificationsComponent', () => {
  let fixture: ComponentFixture<BandeauNotificationsComponent>;
  const etat = signal<EtatNotifications>('a-demander');
  const push = { etat, activer: vi.fn(async () => true) };
  const html = () => fixture.nativeElement as HTMLElement;

  beforeEach(async () => {
    localStorage.clear();
    etat.set('a-demander');
    await TestBed.configureTestingModule({
      imports: [BandeauNotificationsComponent],
      providers: [
        { provide: NotificationPushService, useValue: push },
        { provide: AuthService, useValue: { role: signal('PATIENT') } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(BandeauNotificationsComponent);
    await fixture.whenStable();
  });

  it('explique l\'intérêt des notifications au patient', () => {
    expect(html().textContent).toContain('interaction dangereuse');
  });

  it('ne demande l\'autorisation qu\'au clic sur Activer', async () => {
    expect(push.activer).not.toHaveBeenCalled();
    const boutons = html().querySelectorAll('button');
    (boutons[1] as HTMLButtonElement).click();
    expect(push.activer).toHaveBeenCalled();
  });

  it('« Plus tard » masque le bandeau et s\'en souvient', async () => {
    (html().querySelectorAll('button')[0] as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(html().querySelector('.bandeau')).toBeNull();
    expect(Number(localStorage.getItem('mediscan.notifications.plusTard'))).toBeGreaterThan(Date.now());
  });

  it('n\'apparaît pas si les notifications sont déjà actives ou refusées', async () => {
    etat.set('active');
    await fixture.whenStable();
    expect(html().querySelector('.bandeau')).toBeNull();
  });
});