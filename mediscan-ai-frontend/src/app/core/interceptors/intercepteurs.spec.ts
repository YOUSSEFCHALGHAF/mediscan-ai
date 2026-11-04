import { HttpClient, HttpContext, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';
import { ChargementService } from '../services/chargement.service';
import { MessagesService } from '../services/messages.service';
import { ReseauService } from '../services/reseau.service';
import { authInterceptor } from './auth.interceptor';
import { chargementInterceptor } from './chargement.interceptor';
import { SANS_ERREUR_GLOBALE } from './contexte-http';
import { erreursInterceptor } from './erreurs.interceptor';

describe('Intercepteurs HTTP', () => {
  let http: HttpClient;
  let controle: HttpTestingController;
  const URL_API = `${environment.apiUrl}/patient/profil`;

  // Doublures (faux services) pour observer ce que font les intercepteurs
  const auth = { getToken: vi.fn<() => string | null>(), deconnecter: vi.fn() };
  const messages = { erreur: vi.fn(), succes: vi.fn() };
  const reseau = { enLigne: vi.fn(() => true) };

  beforeEach(() => {
    vi.clearAllMocks();
    auth.getToken.mockReturnValue('jeton-123');
    reseau.enLigne.mockReturnValue(true);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([chargementInterceptor, erreursInterceptor, authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
        { provide: MessagesService, useValue: messages },
        { provide: ReseauService, useValue: reseau },
      ],
    });
    http = TestBed.inject(HttpClient);
    controle = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controle.verify());

  // ---------- authInterceptor ----------
  it('ajoute "Authorization: Bearer" aux requêtes vers notre API', () => {
    http.get(URL_API).subscribe();
    const req = controle.expectOne(URL_API);
    expect(req.request.headers.get('Authorization')).toBe('Bearer jeton-123');
    req.flush({});
  });

  it('n\'envoie JAMAIS le token à un autre site', () => {
    http.get('https://autre-site.example/donnees').subscribe();
    const req = controle.expectOne('https://autre-site.example/donnees');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('déconnecte l\'utilisateur si l\'API répond 401 à une requête avec token', () => {
    http.get(URL_API).subscribe({ error: () => undefined });
    controle.expectOne(URL_API).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.deconnecter).toHaveBeenCalledWith('expiree');
  });

  // ---------- erreursInterceptor ----------
  it('affiche une notification pour une erreur 500', () => {
    http.get(URL_API).subscribe({ error: () => undefined });
    controle.expectOne(URL_API).flush({}, { status: 500, statusText: 'Server Error' });
    expect(messages.erreur).toHaveBeenCalledOnce();
  });

  it('affiche "serveur injoignable" pour une erreur réseau (status 0)', () => {
    http.get(URL_API).subscribe({ error: () => undefined });
    controle.expectOne(URL_API).error(new ProgressEvent('error'));
    expect(messages.erreur).toHaveBeenCalledWith(expect.stringContaining('injoignable'));
  });

  it('ne notifie pas une erreur métier (404) : la page la gère', () => {
    http.get(URL_API).subscribe({ error: () => undefined });
    controle.expectOne(URL_API).flush({}, { status: 404, statusText: 'Not Found' });
    expect(messages.erreur).not.toHaveBeenCalled();
  });

  it('ne notifie pas une erreur réseau quand le navigateur est hors ligne', () => {
    reseau.enLigne.mockReturnValue(false);
    http.get(URL_API).subscribe({ error: () => undefined });
    controle.expectOne(URL_API).error(new ProgressEvent('error'));
    expect(messages.erreur).not.toHaveBeenCalled();
  });

  it('respecte l\'option SANS_ERREUR_GLOBALE', () => {
    http.get(URL_API, { context: new HttpContext().set(SANS_ERREUR_GLOBALE, true) }).subscribe({ error: () => undefined });
    controle.expectOne(URL_API).flush({}, { status: 500, statusText: 'Server Error' });
    expect(messages.erreur).not.toHaveBeenCalled();
  });

  // ---------- chargementInterceptor ----------
  it('active la barre de chargement pendant la requête, puis la désactive', () => {
    const chargement = TestBed.inject(ChargementService);
    http.get(URL_API).subscribe();
    expect(chargement.actif()).toBe(true);
    controle.expectOne(URL_API).flush({});
    expect(chargement.actif()).toBe(false);
  });

  it('désactive aussi la barre de chargement en cas d\'erreur', () => {
    const chargement = TestBed.inject(ChargementService);
    http.get(URL_API).subscribe({ error: () => undefined });
    controle.expectOne(URL_API).flush({}, { status: 500, statusText: 'Server Error' });
    expect(chargement.actif()).toBe(false);
  });
});