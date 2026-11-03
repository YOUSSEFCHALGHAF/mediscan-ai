package com.mediscan.mediscan_ai_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS : autorise UNIQUEMENT le frontend Angular (app.cors.allowed-origins)
 * à appeler l'API depuis le navigateur.
 * Utilisé par Spring Security via http.cors(...) dans SecurityConfig.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> originesAutorisees) {

        CorsConfiguration config = new CorsConfiguration();
        // Origines exactes (jamais "*" pour une application médicale)
        config.setAllowedOrigins(originesAutorisees);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Location"));
        // Token dans l'en-tête Authorization, pas de cookie → pas de credentials
        config.setAllowCredentials(false);
        // Le navigateur garde la réponse du preflight 1 heure
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}