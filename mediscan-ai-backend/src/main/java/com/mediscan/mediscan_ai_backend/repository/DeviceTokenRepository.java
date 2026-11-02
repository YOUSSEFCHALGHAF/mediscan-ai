package com.mediscan.mediscan_ai_backend.repository;

import com.mediscan.mediscan_ai_backend.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    /** Tous les navigateurs d'un utilisateur, pour lui envoyer une notification. */
    List<DeviceToken> findByUtilisateurId(Long utilisateurId);

    Optional<DeviceToken> findByToken(String token);

    /** Supprime un token expiré (signalé par Firebase). */
    @Transactional
    void deleteByToken(String token);
}