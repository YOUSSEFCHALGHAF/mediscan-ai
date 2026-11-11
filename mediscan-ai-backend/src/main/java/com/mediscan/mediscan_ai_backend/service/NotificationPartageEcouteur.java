package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.entity.Notification;
import com.mediscan.mediscan_ai_backend.event.PartageAccordeEvenement;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Prévient le médecin qu'un patient lui a donné accès à ses données.
 * Contenu minimal (pas de nom de patient) : la notification s'affiche sur l'écran verrouillé.
 */
@Component
@RequiredArgsConstructor
public class NotificationPartageEcouteur {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void partageAccorde(PartageAccordeEvenement evenement) {
        notificationService.envoyer(evenement.medecinId(),
                "Nouveau patient",
                "Un patient vous a donné accès à ses données médicales. Retrouvez-le dans « Mes patients ».",
                Notification.TYPE_PARTAGE,
                "/medecin/patients/" + evenement.patientId());
    }
}