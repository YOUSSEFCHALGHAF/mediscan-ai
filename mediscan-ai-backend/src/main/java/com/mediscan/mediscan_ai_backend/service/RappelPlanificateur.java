package com.mediscan.mediscan_ai_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Déclenche l'envoi des rappels à la seconde 0 de chaque minute.
 * Désactivable (app.rappels.planificateur.enabled=false), notamment dans les tests.
 * Limite connue : avec plusieurs serveurs, chacun enverrait les rappels (il faudrait un verrou partagé).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rappels.planificateur.enabled", havingValue = "true", matchIfMissing = true)
public class RappelPlanificateur {

    private final RappelNotificationService rappelNotificationService;

    @Scheduled(cron = "0 * * * * *")
    public void verifierRappels() {
        rappelNotificationService.envoyerRappelsDus(LocalDateTime.now().withSecond(0).withNano(0));
    }
}