package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.entity.Notification;
import com.mediscan.mediscan_ai_backend.entity.Rappel;
import com.mediscan.mediscan_ai_backend.repository.RappelRepository;
import com.mediscan.mediscan_ai_backend.util.PlanificationRappel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Envoi des rappels de prise dont l'heure est arrivée (appelé chaque minute). */
@Slf4j
@Service
@RequiredArgsConstructor
public class RappelNotificationService {

    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private final RappelRepository rappelRepository;
    private final NotificationService notificationService;

    /**
     * @param maintenant instant de référence (paramètre : les tests choisissent l'heure)
     * @return le nombre de rappels envoyés
     */
    @Transactional
    public int envoyerRappelsDus(LocalDateTime maintenant) {
        int envoyes = 0;
        for (Rappel r : rappelRepository.findRappelsActifsEntre(maintenant.toLocalDate().minusDays(1),
                maintenant.toLocalDate())) {
            PlanificationRappel.Creneau creneau = new PlanificationRappel.Creneau(r.getHeurePrise(), r.getFrequence(), r.getDateDebut(), r.getDateFin(),
                    r.isActif(), r.getDerniereNotification());
            if (!PlanificationRappel.estDu(creneau, maintenant)) {
                continue;
            }
            // Le libellé est choisi par le patient : c'est lui qui décide de ce qui s'affiche sur son écran
            notificationService.envoyer(r.getPatient().getId(),
                    "Rappel de prise",
                    "Il est " + r.getHeurePrise().format(HEURE) + " : c'est l'heure de « " + r.getLibelle() + " ».",
                    Notification.TYPE_RAPPEL, "/patient/rappels");
            // Marqué même si aucun navigateur n'a reçu la notification : pas de nouvel essai chaque minute
            r.setDerniereNotification(maintenant);
            envoyes++;
        }
        if (envoyes > 0) {
            log.info("{} rappel(s) de prise envoyé(s) à {}", envoyes, maintenant.format(HEURE));
        }
        return envoyes;
    }
}