import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { environment } from '../../../environments/environment';
import { AuthResponse } from '../models';
import { AuthService } from './auth.service';

/** Réponse de connexion simulée (identique à celle du backend pour Marc). */
const REPONSE_MARC: AuthResponse = {
  token: 'jeton-de-test',
  type: 'Bearer',
  expiresIn: 86_400_000,
  utilisateur: { id: 4, nom: 'Lefèvre', prenom: 'Marc', email: 'marc.lefevre@mediscan.test', role: 'PATIENT' },
};

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('ouvre la session après une connexion réussie', () => {
    service.connecter({ email: 'marc.lefevre@mediscan.test', motDePasse: 'MediScan2026!' }).subscribe();

    const requete = http.expectOne(`${environment.apiUrl}/auth/login`);
    expect(requete.request.method).toBe('POST');
    requete.flush(REPONSE_MARC);

    expect(service.estConnecte()).toBe(true);
    expect(service.role()).toBe('PATIENT');
    expect(service.nomComplet()).toBe('Marc Lefèvre');
    expect(service.getToken()).toBe('jeton-de-test');
    expect(service.cheminEspace()).toBe('/patient');
  });

  it('considère un token expiré comme absent et ferme la session', () => {
    service.connecter({ email: 'x@y.fr', motDePasse: 'x' }).subscribe();
    http.expectOne(`${environment.apiUrl}/auth/login`).flush({ ...REPONSE_MARC, expiresIn: -1000 });

    expect(service.getToken()).toBeNull();
    expect(service.estConnecte()).toBe(false);
  });

  it('vide la session et renvoie vers /connexion à la déconnexion', () => {
    const navigation = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    service.connecter({ email: 'x@y.fr', motDePasse: 'x' }).subscribe();
    http.expectOne(`${environment.apiUrl}/auth/login`).flush(REPONSE_MARC);

    service.deconnecter();

    expect(service.estConnecte()).toBe(false);
    expect(localStorage.getItem('mediscan.token')).toBeNull();
    expect(navigation).toHaveBeenCalledWith(['/connexion'], { queryParams: {} });
  });

  it('dirige un médecin vers /medecin', () => {
    service.connecter({ email: 'x@y.fr', motDePasse: 'x' }).subscribe();
    http.expectOne(`${environment.apiUrl}/auth/login`).flush({
      ...REPONSE_MARC,
      utilisateur: { ...REPONSE_MARC.utilisateur, role: 'MEDECIN' },
    });

    expect(service.cheminEspace()).toBe('/medecin');
  });
});