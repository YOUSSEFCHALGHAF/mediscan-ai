package com.mediscan.mediscan_ai_backend.repository;

import com.mediscan.mediscan_ai_backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUtilisateurIdOrderByDateEnvoiDesc(Long utilisateurId);

    /** Pastille "3 notifications non lues" dans l'interface. */
    long countByUtilisateurIdAndLueFalse(Long utilisateurId);
}