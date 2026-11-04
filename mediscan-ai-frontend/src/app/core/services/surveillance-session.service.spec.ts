import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { AuthService } from './auth.service';
import { MessagesService } from './messages.service';
import { DELAI_AVERTISSEMENT_MS, SurveillanceSessionService } from './surveillance-session.service';

describe('SurveillanceSessionService', () => {
  const expiration = signal<number | null>(null);
  const auth = { expiration, deconnecter: vi.fn() };
  const messages = { avertissement: vi.fn() };

  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    expiration.set(null);
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: MessagesService, useValue: messages },
      ],
    });
    TestBed.inject(SurveillanceSessionService);
  });

  afterEach(() => vi.useRealTimers());

  it('avertit 5 minutes avant l\'expiration puis déconnecte à l\'expiration', () => {
    expiration.set(Date.now() + 30 * 60 * 1000); // session de 30 minutes
    TestBed.tick(); // exécute l'effect

    vi.advanceTimersByTime(30 * 60 * 1000 - DELAI_AVERTISSEMENT_MS - 1000);
    expect(messages.avertissement).not.toHaveBeenCalled();

    vi.advanceTimersByTime(2000);
    expect(messages.avertissement).toHaveBeenCalledWith(expect.stringContaining('5 minute'), 15000);
    expect(auth.deconnecter).not.toHaveBeenCalled();

    vi.advanceTimersByTime(DELAI_AVERTISSEMENT_MS);
    expect(auth.deconnecter).toHaveBeenCalledWith('expiree');
  });

  it('ne fait rien après une déconnexion', () => {
    expiration.set(Date.now() + 10 * 60 * 1000);
    TestBed.tick();
    expiration.set(null); // déconnexion
    TestBed.tick();

    vi.advanceTimersByTime(60 * 60 * 1000);
    expect(messages.avertissement).not.toHaveBeenCalled();
    expect(auth.deconnecter).not.toHaveBeenCalled();
  });
});