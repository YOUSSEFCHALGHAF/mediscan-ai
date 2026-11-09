import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Service, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { SANS_CHARGEMENT, SANS_ERREUR_GLOBALE } from '../interceptors/contexte-http';
import { AlerteService } from '../services/alerte.service';
import { MessagesService } from '../services/messages.service';
import { CONFIG_FIREBASE, firebaseConfigure } from './config-firebase';
import { FirebaseClient, MessageRecu } from './firebase-client';
import { decrireNavigateur } from './navigateur';

/**
 * État des notifications sur CE navigateur :
 * - non-configure : clés Firebase absentes (l'application fonctionne sans notifications) ;
 * - non-supporte  : navigateur incompatible (ou page en HTTP hors localhost) ;
 * - a-demander    : l'utilisateur n'a pas encore répondu ;
 * - refuse        : l'utilisateur a bloqué les notifications ;
 * - active        : token enregistré auprès du serveur ;
 * - erreur        : échec technique (réseau, configuration...).
 */
export type EtatNotifications = 'chargement' | 'non-configure' | 'non-supporte' | 'a-demander' | 'refuse' | 'active' | 'erreur';

/** Requêtes en arrière-plan : ni barre de chargement, ni message d'erreur global. */
const DISCRET = () => new HttpContext().set(SANS_CHARGEMENT, true).set(SANS_ERREUR_GLOBALE, true);

/**
 * Notifications Web Push (Firebase Cloud Messaging) :
 * autorisation, enregistrement du token auprès du serveur, affichage au premier plan, désinscription.
 */
@Service()
export class NotificationPushService {
  private readonly http = inject(HttpClient);
  private readonly firebase = inject(FirebaseClient);
  private readonly config = inject(CONFIG_FIREBASE);
  private readonly messages = inject(MessagesService);
  private readonly alerteService = inject(AlerteService);
  private readonly url = `${environment.apiUrl}/notifications/tokens`;

  private readonly _etat = signal<EtatNotifications>('chargement');
  readonly etat = this._etat.asReadonly();

  private token: string | null = null;
  private arreterEcoute: (() => void) | null = null;

  /**
   * Appelé après la connexion : détermine l'état SANS rien demander à l'utilisateur.
   * Si l'autorisation a déjà été donnée, le token est renouvelé en silence (il peut changer).
   */
  async initialiser(): Promise<void> {
    if (!firebaseConfigure(this.config)) {
      this._etat.set('non-configure');
      return;
    }
    if (!(await this.firebase.estSupporte())) {
      this._etat.set('non-supporte');
      return;
    }
    switch (Notification.permission) {
      case 'denied':
        this._etat.set('refuse');
        return;
      case 'default':
        this._etat.set('a-demander');
        return;
      default:
        await this.enregistrer();
    }
  }

  /** Demande l'autorisation (UNIQUEMENT suite à un clic de l'utilisateur), puis enregistre le token. */
  async activer(): Promise<boolean> {
    const reponse = await Notification.requestPermission();
    if (reponse !== 'granted') {
      this._etat.set(reponse === 'denied' ? 'refuse' : 'a-demander');
      return false;
    }
    await this.enregistrer();
    if (this.etat() === 'active') {
      this.messages.succes('Notifications activées sur ce navigateur');
      return true;
    }
    this.messages.erreur('Impossible d\'activer les notifications pour le moment.');
    return false;
  }

  /** Déconnexion : le serveur oublie ce navigateur (il ne recevra plus les notifications de ce compte). */
  async desinscrire(): Promise<void> {
    this.arreterEcoute?.();
    this.arreterEcoute = null;
    const token = this.token;
    this.token = null;
    if (!token) {
      return;
    }
    try {
      await firstValueFrom(this.http.delete(this.url, { params: { token }, context: DISCRET() }));
    } catch {
      // le serveur nettoiera de lui-même les tokens devenus invalides
    }
    try {
      await this.firebase.supprimerToken();
    } catch {
      // sans conséquence
    }
  }

  private async enregistrer(): Promise<void> {
    try {
      const token = await this.firebase.obtenirToken(this.config);
      await firstValueFrom(
        this.http.post(this.url, { token, navigateur: decrireNavigateur(navigator.userAgent) }, { context: DISCRET() }),
      );
      this.token = token;
      this.arreterEcoute?.();
      this.arreterEcoute = await this.firebase.ecouter((message) => this.afficherPremierPlan(message));
      this._etat.set('active');
    } catch (erreur) {
      console.error('Notifications : enregistrement impossible', erreur);
      this._etat.set('erreur');
    }
  }

  /** Application ouverte : la notification s'affiche dans l'application (le système ne l'affiche pas). */
  private afficherPremierPlan(message: MessageRecu): void {
    const texte = message.corps ? `${message.titre} : ${message.corps}` : message.titre;
    if (message.type === 'ALERTE') {
      this.messages.avertissement(texte, 15000);
      this.alerteService.actualiserCompteur();
    } else {
      this.messages.info(texte);
    }
  }
}