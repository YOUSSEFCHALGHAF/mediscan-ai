package com.mediscan.mediscan_ai_backend.service;

import com.mediscan.mediscan_ai_backend.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Données de démonstration : à 20:00, « Coumadine du soir » (Marc) et « Glucophage dîner » (Nadia) ;
 * à 08:00, « Glucophage petit-déjeuner » (Nadia). Le rappel ponctuel de Yasmine est inactif.
 */
@SpringBootTest(properties = {"app.firebase.enabled=false", "app.rappels.planificateur.enabled=false"})
@Transactional
class RappelNotificationServiceIntegrationTests {

    private static final long MARC = 4L;

    @Autowired private RappelNotificationService rappelNotificationService;
    @Autowired private NotificationRepository notificationRepository;

    @Test
    @DisplayName("À 20:00, les deux rappels du soir sont envoyés")
    void rappelsDuSoir() {
        long avant = notificationRepository.countByUtilisateurIdAndLueFalse(MARC);

        int envoyes = rappelNotificationService.envoyerRappelsDus(LocalDateTime.of(2026, 11, 4, 20, 0));

        assertThat(envoyes).isEqualTo(2);
        assertThat(notificationRepository.countByUtilisateurIdAndLueFalse(MARC)).isEqualTo(avant + 1);
    }

    @Test
    @DisplayName("Une minute plus tard : rien n'est renvoyé (derniere_notification)")
    void pasDeDoublon() {
        rappelNotificationService.envoyerRappelsDus(LocalDateTime.of(2026, 11, 4, 20, 0));

        assertThat(rappelNotificationService.envoyerRappelsDus(LocalDateTime.of(2026, 11, 4, 20, 1))).isZero();
    }

    @Test
    @DisplayName("À 08:00, seul le rappel du matin de Nadia est envoyé")
    void rappelDuMatin() {
        assertThat(rappelNotificationService.envoyerRappelsDus(LocalDateTime.of(2026, 11, 4, 8, 0))).isEqualTo(1);
    }
}