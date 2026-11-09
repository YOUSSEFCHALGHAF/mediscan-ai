package com.mediscan.mediscan_ai_backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Connexion à Firebase Admin, UNIQUEMENT si app.firebase.enabled=true.
 * Sinon, aucun bean FirebaseMessaging n'existe : NotificationService journalise sans envoyer.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true")
public class FirebaseConfig {

    private static final String NOM_APPLICATION = "mediscan";

    @Bean
    public FirebaseMessaging firebaseMessaging(@Value("${app.firebase.credentials-path}") Resource compteDeService)
            throws IOException {
        if (!compteDeService.exists()) {
            throw new IllegalStateException("Compte de service Firebase introuvable : " + compteDeService
                    + ". Placez le fichier JSON dans le dossier secrets/ ou mettez app.firebase.enabled=false.");
        }
        FirebaseApp app = FirebaseApp.getApps().stream()
                .filter(a -> a.getName().equals(NOM_APPLICATION))
                .findFirst()
                .orElseGet(() -> initialiser(compteDeService));
        log.info("Firebase Cloud Messaging activé (projet {})", app.getOptions().getProjectId());
        return FirebaseMessaging.getInstance(app);
    }

    private static FirebaseApp initialiser(Resource compteDeService) {
        try (InputStream flux = compteDeService.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(flux))
                    .build();
            return FirebaseApp.initializeApp(options, NOM_APPLICATION);
        } catch (IOException e) {
            throw new IllegalStateException("Compte de service Firebase illisible : " + e.getMessage(), e);
        }
    }
}