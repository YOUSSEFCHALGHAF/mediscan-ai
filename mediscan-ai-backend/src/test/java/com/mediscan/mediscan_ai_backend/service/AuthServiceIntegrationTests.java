package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.dto.request.LoginRequest;
import com.mediscan.mediscan_ai_backend.dto.request.RegisterPatientRequest;
import com.mediscan.mediscan_ai_backend.dto.response.AuthResponse;
import com.mediscan.mediscan_ai_backend.entity.Patient;
import com.mediscan.mediscan_ai_backend.entity.enums.Role;
import com.mediscan.mediscan_ai_backend.exception.ConflitException;
import com.mediscan.mediscan_ai_backend.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prérequis : database/02_donnees_test.sql exécuté. Chaque test est annulé (rollback). */
@SpringBootTest
@Transactional
class AuthServiceIntegrationTests {

    @Autowired private AuthService authService;
    @Autowired private PatientRepository patientRepository;

    @Test
    @DisplayName("Connexion de Marc avec le mot de passe de démo : token + rôle PATIENT")
    void connexionCompteDemo() {
        AuthResponse rep = authService.connecter(
                new LoginRequest("marc.lefevre@mediscan.test", "MediScan2026!"));

        assertThat(rep.token()).isNotBlank();
        assertThat(rep.type()).isEqualTo("Bearer");
        assertThat(rep.utilisateur().role()).isEqualTo(Role.PATIENT);
    }

    @Test
    @DisplayName("Connexion avec email en majuscules et espaces : acceptée")
    void connexionEmailNormalise() {
        AuthResponse rep = authService.connecter(
                new LoginRequest("  KARIM.MANSOUR@mediscan.test ", "MediScan2026!"));

        assertThat(rep.utilisateur().role()).isEqualTo(Role.MEDECIN);
        assertThat(rep.utilisateur().specialite()).isEqualTo("Cardiologie");
    }

    @Test
    @DisplayName("Mauvais mot de passe : BadCredentialsException")
    void mauvaisMotDePasse() {
        assertThatThrownBy(() -> authService.connecter(
                new LoginRequest("marc.lefevre@mediscan.test", "faux1234")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Inscription : le mot de passe est stocké hashé avec BCrypt")
    void inscriptionHashBcrypt() {
        authService.inscrirePatient(nouvelleInscription("nouveau.patient@mediscan.test"));

        Patient p = patientRepository.findByEmailIgnoreCase("nouveau.patient@mediscan.test").orElseThrow();
        assertThat(p.getMotDePasseHash()).startsWith("$2a$10$").isNotEqualTo("Patient2026");
        assertThat(p.getRole()).isEqualTo(Role.PATIENT);
    }

    @Test
    @DisplayName("Inscription avec un email déjà utilisé : ConflitException")
    void emailDejaUtilise() {
        assertThatThrownBy(() -> authService.inscrirePatient(
                nouvelleInscription("marc.lefevre@mediscan.test")))
                .isInstanceOf(ConflitException.class)
                .hasMessageContaining("existe déjà");
    }

    private RegisterPatientRequest nouvelleInscription(String email) {
        return new RegisterPatientRequest("Nouveau", "Patient", email, "Patient2026",
                LocalDate.of(1990, 1, 1), "HOMME", "A+", null);
    }
}