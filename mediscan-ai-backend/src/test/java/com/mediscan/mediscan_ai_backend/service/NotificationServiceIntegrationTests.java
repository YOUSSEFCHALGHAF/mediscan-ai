package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.repository.DeviceTokenRepository;
import com.mediscan.mediscan_ai_backend.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** Firebase désactivé ici : on teste la logique (tokens, historique) sans rien envoyer. */
@SpringBootTest(properties = {"app.firebase.enabled=false", "app.rappels.planificateur.enabled=false"})
@Transactional
class NotificationServiceIntegrationTests {

    private static final long YASMINE = 3L;
    private static final long MARC = 4L;
    private static final String TOKEN = "token-de-test-navigateur-chrome";

    @Autowired private NotificationService notificationService;
    @Autowired private DeviceTokenRepository tokenRepository;
    @Autowired private NotificationRepository notificationRepository;

    @Test
    @DisplayName("Enregistre le token d'un navigateur")
    void enregistreToken() {
        notificationService.enregistrerToken(MARC, TOKEN, "Chrome 142 / Windows");

        assertThat(tokenRepository.findByUtilisateurId(MARC)).extracting("token").containsExactly(TOKEN);
    }

    @Test
    @DisplayName("Un autre compte qui se connecte sur ce navigateur récupère le token (pas de doublon)")
    void tokenChangeDeProprietaire() {
        notificationService.enregistrerToken(MARC, TOKEN, "Chrome");
        notificationService.enregistrerToken(YASMINE, TOKEN, "Chrome");

        assertThat(tokenRepository.findByUtilisateurId(MARC)).isEmpty();
        assertThat(tokenRepository.findByUtilisateurId(YASMINE)).hasSize(1);
    }

    @Test
    @DisplayName("Un utilisateur ne peut pas supprimer le token d'un autre")
    void suppressionProtegee() {
        notificationService.enregistrerToken(MARC, TOKEN, "Chrome");

        notificationService.supprimerToken(YASMINE, TOKEN);
        assertThat(tokenRepository.findByToken(TOKEN)).isPresent();

        notificationService.supprimerToken(MARC, TOKEN);
        assertThat(tokenRepository.findByToken(TOKEN)).isEmpty();
    }

    @Test
    @DisplayName("Sans Firebase : la notification est enregistrée dans l'historique, rien n'est envoyé")
    void historiqueSansFirebase() {
        long avant = notificationRepository.countByUtilisateurIdAndLueFalse(MARC);

        int envoyes = notificationService.envoyer(MARC, "Titre", "Message", "ALERTE", "/patient/alertes");

        assertThat(envoyes).isZero();
        assertThat(notificationRepository.countByUtilisateurIdAndLueFalse(MARC)).isEqualTo(avant + 1);
    }
}