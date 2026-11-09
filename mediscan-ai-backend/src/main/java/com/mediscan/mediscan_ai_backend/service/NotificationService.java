package com.mediscan.mediscan_ai_backend.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import com.google.firebase.messaging.WebpushNotification;

import com.mediscan.mediscan_ai_backend.entity.DeviceToken;
import com.mediscan.mediscan_ai_backend.entity.Notification;
import com.mediscan.mediscan_ai_backend.repository.DeviceTokenRepository;
import com.mediscan.mediscan_ai_backend.repository.NotificationRepository;
import com.mediscan.mediscan_ai_backend.repository.UtilisateurRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notifications : historique en base + envoi Web Push (Firebase Cloud Messaging)
 * sur tous les navigateurs de l'utilisateur. Fonctionne aussi SANS Firebase (journalisation seule).
 */
@Slf4j
@Service
public class NotificationService {

    private final DeviceTokenRepository tokenRepository;
    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ObjectProvider<FirebaseMessaging> firebase;
    private final String urlFrontend;

    public NotificationService(DeviceTokenRepository tokenRepository,
                               NotificationRepository notificationRepository,
                               UtilisateurRepository utilisateurRepository,
                               ObjectProvider<FirebaseMessaging> firebase,
                               @Value("${app.frontend.url:http://localhost:4200}") String urlFrontend) {
        this.tokenRepository = tokenRepository;
        this.notificationRepository = notificationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.firebase = firebase;
        this.urlFrontend = urlFrontend;
    }

    /**
     * Enregistre (ou rattache) le token d'un navigateur.
     * Si un autre compte s'était connecté sur ce navigateur, le token change de propriétaire :
     * l'ancien compte ne reçoit plus de notifications ici.
     */
    @Transactional
    public void enregistrerToken(Long utilisateurId, String token, String navigateur) {
        DeviceToken deviceToken = tokenRepository.findByToken(token).orElseGet(DeviceToken::new);
        deviceToken.setToken(token);
        deviceToken.setUtilisateur(utilisateurRepository.getReferenceById(utilisateurId));
        deviceToken.setNavigateur(navigateur);
        deviceToken.setDateEnregistrement(LocalDateTime.now());
        tokenRepository.save(deviceToken);
        log.info("Token de notification enregistré pour l'utilisateur {} ({})", utilisateurId, navigateur);
    }

    /** Déconnexion : oublie ce navigateur (seulement s'il appartient bien à cet utilisateur). */
    @Transactional
    public void supprimerToken(Long utilisateurId, String token) {
        tokenRepository.findByToken(token)
                .filter(t -> t.getUtilisateur().getId().equals(utilisateurId))
                .ifPresent(tokenRepository::delete);
    }

    /**
     * Enregistre la notification dans l'historique, puis l'envoie sur tous les navigateurs de l'utilisateur.
     *
     * @param chemin page à ouvrir au clic (ex. "/patient/alertes")
     * @return le nombre de navigateurs ayant reçu la notification
     */
    @Transactional
    public int envoyer(Long utilisateurId, String titre, String message, String type, String chemin) {
        Notification historique = new Notification();
        historique.setUtilisateur(utilisateurRepository.getReferenceById(utilisateurId));
        historique.setTitre(tronquer(titre, 150));
        historique.setMessage(tronquer(message, 500));
        historique.setType(type);
        notificationRepository.save(historique);

        FirebaseMessaging messaging = firebase.getIfAvailable();
        if (messaging == null) {
            log.info("[Notifications désactivées] {} pour l'utilisateur {} : {} - {}", type, utilisateurId, titre, message);
            return 0;
        }
        List<String> tokens = tokenRepository.findByUtilisateurId(utilisateurId).stream()
                .map(DeviceToken::getToken).toList();
        if (tokens.isEmpty()) {
            return 0;
        }

        try {
            BatchResponse reponse = messaging.sendEachForMulticast(construireMessage(tokens, titre, message, type, chemin));
            nettoyerTokensExpires(reponse, tokens);
            log.info("Notification {} envoyée à l'utilisateur {} : {}/{} navigateur(s)",
                    type, utilisateurId, reponse.getSuccessCount(), tokens.size());
            return reponse.getSuccessCount();
        } catch (FirebaseMessagingException e) {
            log.warn("Envoi Firebase impossible pour l'utilisateur {} : {}", utilisateurId, e.getMessage());
            return 0;
        }
    }

    private MulticastMessage construireMessage(List<String> tokens, String titre, String message,
                                               String type, String chemin) {
        WebpushConfig.Builder webpush = WebpushConfig.builder()
                .setNotification(WebpushNotification.builder()
                        .setTitle(titre)
                        .setBody(message)
                        .setIcon("/favicon.ico")
                        .setTag(type)                                     // remplace la notification précédente du même type
                        .setRequireInteraction(Notification.TYPE_ALERTE.equals(type)) // une alerte reste affichée
                        .build());
        // Firebase exige un lien HTTPS : en développement (http://localhost), c'est le service worker
        // qui ouvre la page indiquée dans data.lien
        String lien = urlFrontend + chemin;
        if (lien.startsWith("https://")) {
            webpush.setFcmOptions(WebpushFcmOptions.withLink(lien));
        }
        return MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(titre).setBody(message).build())
                .putData("type", type)
                .putData("lien", chemin)
                .setWebpushConfig(webpush.build())
                .build();
    }

    /**
     * Supprime les tokens que Firebase déclare expirés (navigateur désinstallé, autorisation retirée...).
     * INVALID_ARGUMENT peut aussi signifier que le MESSAGE est invalide (pour tous les tokens) :
     * on ne supprime alors un token que si au moins un autre envoi a réussi.
     */
    private void nettoyerTokensExpires(BatchResponse reponse, List<String> tokens) {
        List<SendResponse> resultats = reponse.getResponses();
        boolean messageValide = reponse.getSuccessCount() > 0;
        for (int i = 0; i < resultats.size(); i++) {
            SendResponse resultat = resultats.get(i);
            if (resultat.isSuccessful() || resultat.getException() == null) {
                continue;
            }
            MessagingErrorCode code = resultat.getException().getMessagingErrorCode();
            boolean expire = code == MessagingErrorCode.UNREGISTERED
                    || code == MessagingErrorCode.SENDER_ID_MISMATCH
                    || (code == MessagingErrorCode.INVALID_ARGUMENT && messageValide);
            if (expire) {
                tokenRepository.deleteByToken(tokens.get(i));
                log.info("Token de notification expiré supprimé ({})", code);
            }
        }
    }

    private static String tronquer(String texte, int max) {
        return texte.length() <= max ? texte : texte.substring(0, max - 1) + "…";
    }
}