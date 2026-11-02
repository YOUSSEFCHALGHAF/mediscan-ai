package com.mediscan.mediscan_ai_backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration de sécurité TEMPORAIRE (étape 2).
 * Sera remplacée à l'étape 5 par la configuration JWT complète.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API REST sans session ni formulaire : la protection CSRF n'est pas utile ici
                // (le token JWT sera envoyé dans l'en-tête Authorization, pas dans un cookie).
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**").permitAll()   // accessible sans connexion
                        .anyRequest().authenticated()                    // tout le reste est protégé
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}