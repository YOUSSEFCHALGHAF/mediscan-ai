import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { firstValueFrom, isObservable, of } from 'rxjs';

import { AvecModificationsNonEnregistrees, modificationsNonEnregistreesGuard } from './modifications.guard';

describe('modificationsNonEnregistreesGuard', () => {
  const dialog = { open: vi.fn() };

  const executer = (modifie: boolean) => {
    const composant: AvecModificationsNonEnregistrees = { aDesModificationsNonEnregistrees: () => modifie };
    return TestBed.runInInjectionContext(() =>
      modificationsNonEnregistreesGuard(
        composant,
        {} as ActivatedRouteSnapshot,
        {} as RouterStateSnapshot,
        {} as RouterStateSnapshot,
      ),
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({ providers: [{ provide: MatDialog, useValue: dialog }] });
  });

  it('laisse partir sans question quand il n\'y a pas de modification', () => {
    expect(executer(false)).toBe(true);
    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('demande confirmation et reste sur la page si l\'utilisateur annule', async () => {
    dialog.open.mockReturnValue({ afterClosed: () => of(false) });
    const resultat = executer(true);
    expect(isObservable(resultat)).toBe(true);
    expect(await firstValueFrom(resultat as never)).toBe(false);
  });

  it('quitte la page si l\'utilisateur confirme', async () => {
    dialog.open.mockReturnValue({ afterClosed: () => of(true) });
    expect(await firstValueFrom(executer(true) as never)).toBe(true);
  });
});