package com.mediscan.mediscan_ai_backend.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Configuration de sécurité  – CORS (CorsConfig) + preflight OPTIONS autorisées
 * - API sans session (STATELESS), authentification par token JWT
 * — routes publiques : /api/public/**, inscription, connexion
 * - /api/patient/** réservé au rôle PATIENT, /api/medecin/** au rôle MEDECIN
 * — erreurs 401 / 403 renvoyées en JSON
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS : utilise le bean CorsConfigurationSource de CorsConfig
                .cors(Customizer.withDefaults())
                // Token dans l'en-tête Authorization (pas de cookie) → CSRF inutile
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())

                .authorizeHttpRequests(auth -> auth
                        // Preflight CORS du navigateur : toujours autorisé
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register/patient",
                                "/api/auth/register/medecin",
                                "/api/auth/login").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Par rôle
                        .requestMatchers("/api/patient/**").hasRole("PATIENT")
                        .requestMatchers("/api/medecin/**").hasRole("MEDECIN")
                        // Tout le reste (dont /api/medicaments/**, /api/auth/me) : connecté
                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                ecrireErreur(response, 401, "Unauthorized",
                                        "Authentification requise : token absent, invalide ou expiré",
                                        request.getRequestURI()))
                        .accessDeniedHandler((request, response, e) ->
                                ecrireErreur(response, 403, "Forbidden",
                                        "Accès refusé : cette ressource n'est pas accessible avec votre rôle",
                                        request.getRequestURI()))
                )

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Empêche Spring Boot d'enregistrer JwtAuthFilter une 2e fois comme filtre global. */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> desactiverEnregistrementAuto(JwtAuthFilter filtre) {
        FilterRegistrationBean<JwtAuthFilter> enregistrement = new FilterRegistrationBean<>(filtre);
        enregistrement.setEnabled(false);
        return enregistrement;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    private static void ecrireErreur(HttpServletResponse response, int statut, String erreur,
                                     String message, String chemin) throws IOException {
        response.setStatus(statut);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String json = String.format(
                "{\"status\":%d,\"erreur\":\"%s\",\"message\":\"%s\",\"chemin\":\"%s\",\"timestamp\":\"%s\"}",
                statut, erreur, message.replace("\"", "'"), chemin.replace("\"", ""), Instant.now());
        response.getWriter().write(json);
    }
}