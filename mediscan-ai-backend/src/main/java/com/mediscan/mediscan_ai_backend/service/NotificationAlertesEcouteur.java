package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.entity.Notification;
import com.mediscan.mediscan_ai_backend.entity.PartageDonnees;
import com.mediscan.mediscan_ai_backend.event.AlertesGravesEvenement;
import com.mediscan.mediscan_ai_backend.repository.PartageDonneesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envoie les notifications d'alertes graves  – AFTER_COMMIT : uniquement si les alertes ont bien été enregistrées (pas d'alerte "fantôme") ;
 * - @Async : en tâche de fond, le scan du patient n'attend pas la réponse de Google.
 *
 * Contenu volontairement MINIMAL : une notification transitée par Google et s'affiche sur l'écran
 * verrouillé. Le détail médical reste dans l'application, accessible après connexion (RGPD).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationAlertesEcouteur {

    private final NotificationService notificationService;
    private final PartageDonneesRepository partageRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alertesGraves(AlertesGravesEvenement evenement) {
        Long patientId = evenement.patientId();

        notificationService.envoyer(patientId,
                "Alerte de sécurité médicamenteuse",
                "Une alerte importante concerne votre dernier scan. Ouvrez MediScan avant de prendre ce médicament.",
                Notification.TYPE_ALERTE, "/patient/alertes");

        // Médecins ayant un consentement ACTIF qui couvre les alertes de ce patient
        partageRepository.findByPatientIdOrderByDateConsentementDesc(patientId).stream()
                .filter(PartageDonnees::isActif)
                .filter(p -> p.couvre(PartageDonnees.ALERTES))
                .forEach(p -> notificationService.envoyer(p.getMedecin().getId(),
                        "Nouvelle alerte grave",
                        "Un de vos patients a une nouvelle alerte de gravité élevée. Ouvrez MediScan pour la consulter.",
                        Notification.TYPE_ALERTE, "/medecin/patients/" + patientId));

        log.info("Notifications d'alerte grave envoyées pour le patient {} ({} alerte(s))", patientId, evenement.nombre());
    }
}