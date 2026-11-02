package com.mediscan.mediscan_ai_backend.security;

import com.mediscan.mediscan_ai_backend.entity.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Test unitaire pur : aucune base, aucun Spring. */
class JwtServiceTest {

    /** Clé de test de 64 octets (jamais utilisée ailleurs). */
    private static final String CLE_TEST = Base64.getEncoder()
            .encodeToString("cle-de-test-mediscan-ai-uniquement-pour-les-tests-unitaires-64o!".getBytes());

    private JwtService jwtService;
    private Patient patient;

    @BeforeEach
    void preparer() {
        jwtService = new JwtService(CLE_TEST, 3_600_000); // 1 heure
        patient = new Patient();
        patient.setId(4L);
        patient.setNom("Lefèvre");
        patient.setPrenom("Marc");
        patient.setEmail("marc.lefevre@mediscan.test");
        patient.setMotDePasseHash("hash");
    }

    @Test
    @DisplayName("Un token généré contient l'email et est valide pour son propriétaire")
    void tokenValide() {
        String token = jwtService.genererToken(patient);

        assertThat(jwtService.extraireEmail(token)).isEqualTo("marc.lefevre@mediscan.test");
        assertThat(jwtService.estValide(token, new UtilisateurPrincipal(patient))).isTrue();
    }

    @Test
    @DisplayName("Un token modifié est refusé (signature invalide)")
    void tokenFalsifie() {
        String token = jwtService.genererToken(patient);
        String falsifie = token.substring(0, token.length() - 4) + "abcd";

        assertThat(jwtService.estValide(falsifie, new UtilisateurPrincipal(patient))).isFalse();
    }

    @Test
    @DisplayName("Un token expiré est refusé")
    void tokenExpire() {
        JwtService serviceExpire = new JwtService(CLE_TEST, -1000); // expiré dès sa création
        String token = serviceExpire.genererToken(patient);

        assertThat(serviceExpire.estValide(token, new UtilisateurPrincipal(patient))).isFalse();
    }

    @Test
    @DisplayName("Un token d'un autre utilisateur est refusé")
    void tokenAutreUtilisateur() {
        String token = jwtService.genererToken(patient);
        Patient autre = new Patient();
        autre.setId(3L);
        autre.setNom("Haddad");
        autre.setPrenom("Yasmine");
        autre.setEmail("yasmine.haddad@mediscan.test");
        autre.setMotDePasseHash("hash");

        assertThat(jwtService.estValide(token, new UtilisateurPrincipal(autre))).isFalse();
    }

    @Test
    @DisplayName("Une clé trop courte est refusée au démarrage")
    void cleTropCourte() {
        String cleCourte = Base64.getEncoder().encodeToString("trop-courte".getBytes());

        assertThatThrownBy(() -> new JwtService(cleCourte, 1000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("trop courte");
    }
}