import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { MessagesService } from '../services/messages.service';
import { CONFIG_FIREBASE, ConfigFirebase } from './config-firebase';
import { FirebaseClient, MessageRecu } from './firebase-client';
import { NotificationPushService } from './notification-push.service';

const CONFIG: ConfigFirebase = {
  apiKey: 'cle', authDomain: 'mediscan.firebaseapp.com', projectId: 'mediscan', storageBucket: '',
  messagingSenderId: '123', appId: '1:123:web:abc', vapidKey: 'vapid',
};

describe('NotificationPushService', () => {
  let service: NotificationPushService;
  let http: HttpTestingController;
  let ecouteur: ((m: MessageRecu) => void) | null;
  const messages = { succes: vi.fn(), erreur: vi.fn(), info: vi.fn(), avertissement: vi.fn() };
  const firebase = {
    estSupporte: vi.fn(async () => true),
    obtenirToken: vi.fn(async () => 'token-du-navigateur'),
    ecouter: vi.fn(async (rappel: (m: MessageRecu) => void) => { ecouteur = rappel; return () => undefined; }),
    supprimerToken: vi.fn(async () => undefined),
  };
  const URL_TOKENS = `${environment.apiUrl}/notifications/tokens`;

  function configurer(config: ConfigFirebase, permission: NotificationPermission): void {
    vi.clearAllMocks();
    ecouteur = null;
    vi.stubGlobal('Notification', { permission, requestPermission: vi.fn(async () => 'granted') });
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: CONFIG_FIREBASE, useValue: config },
        { provide: FirebaseClient, useValue: firebase },
        { provide: MessagesService, useValue: messages },
      ],
    });
    service = TestBed.inject(NotificationPushService);
    http = TestBed.inject(HttpTestingController);
  }

  /** Laisse s'exécuter les promesses en attente. */
  const attendre = () => new Promise((r) => setTimeout(r));

  afterEach(() => {
    http.verify();
    vi.unstubAllGlobals();
  });

  it('reste désactivé, sans erreur, quand Firebase n\'est pas configuré', async () => {
    configurer({ ...CONFIG, apiKey: '' }, 'default');
    await service.initialiser();
    expect(service.etat()).toBe('non-configure');
    expect(firebase.estSupporte).not.toHaveBeenCalled();
  });

  it('ne demande RIEN à l\'initialisation : attend un clic de l\'utilisateur', async () => {
    configurer(CONFIG, 'default');
    await service.initialiser();
    expect(service.etat()).toBe('a-demander');
    expect(Notification.requestPermission).not.toHaveBeenCalled();
  });

  it('respecte un refus', async () => {
    configurer(CONFIG, 'denied');
    await service.initialiser();
    expect(service.etat()).toBe('refuse');
  });

  it('après accord : enregistre le token auprès du serveur puis affiche les notifications reçues', async () => {
    configurer(CONFIG, 'default');
    const resultat = service.activer();
    await attendre();

    const requete = http.expectOne(URL_TOKENS);
    expect(requete.request.method).toBe('POST');
    expect(requete.request.body.token).toBe('token-du-navigateur');
    requete.flush(null);
    expect(await resultat).toBe(true);
    expect(service.etat()).toBe('active');

    // Une alerte reçue au premier plan s'affiche et met à jour la pastille du menu
    ecouteur!({ titre: 'Alerte médicamenteuse', corps: 'Interaction warfarine', type: 'ALERTE', lien: null });
    expect(messages.avertissement).toHaveBeenCalledWith('Alerte médicamenteuse : Interaction warfarine', 15000);
    http.expectOne(`${environment.apiUrl}/patient/alertes`).flush([]);
  });

  it('à la déconnexion, demande au serveur d\'oublier ce navigateur', async () => {
    configurer(CONFIG, 'granted');
    const init = service.initialiser();
    await attendre();
    http.expectOne(URL_TOKENS).flush(null);
    await init;

    const fin = service.desinscrire();
    await attendre();
    const suppression = http.expectOne((r) => r.method === 'DELETE' && r.url === URL_TOKENS);
    expect(suppression.request.params.get('token')).toBe('token-du-navigateur');
    suppression.flush(null);
    await fin;
    expect(firebase.supprimerToken).toHaveBeenCalled();
  });
});