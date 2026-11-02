package com.mediscan.mediscan_ai_backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint public de vérification de l'état du backend.
 * GET…<a href="http://localhost:8080/api/public/health"></a>
 */
@Slf4j                      // Lombok : crée automatiquement le logger "log"
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor    // Lombok : crée le constructeur pour injecter dataSource
public class HealthController {

    private final DataSource dataSource;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean baseConnectee;
        try (Connection connexion = dataSource.getConnection()) {
            baseConnectee = connexion.isValid(2);
        } catch (Exception e) {
            log.error("Connexion MySQL impossible : {}", e.getMessage());
            baseConnectee = false;
        }

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("status", "UP");
        reponse.put("application", "MediaScan AI Backend");
        reponse.put("database", baseConnectee ? "CONNECTÉE" : "INDISPONIBLE");
        reponse.put("timestamp", Instant.now().toString());

        log.debug("Health check appelé - base : {}", reponse.get("database"));
        return ResponseEntity.ok(reponse);
    }
}