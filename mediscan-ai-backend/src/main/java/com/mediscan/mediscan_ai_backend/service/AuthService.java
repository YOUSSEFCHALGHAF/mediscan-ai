package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.dto.request.LoginRequest;
import com.mediscan.mediscan_ai_backend.dto.request.RegisterMedecinRequest;
import com.mediscan.mediscan_ai_backend.dto.request.RegisterPatientRequest;
import com.mediscan.mediscan_ai_backend.dto.response.AuthResponse;
import com.mediscan.mediscan_ai_backend.dto.response.UtilisateurResponse;
import com.mediscan.mediscan_ai_backend.entity.Medecin;
import com.mediscan.mediscan_ai_backend.entity.Patient;
import com.mediscan.mediscan_ai_backend.entity.Utilisateur;
import com.mediscan.mediscan_ai_backend.exception.ConflitException;
import com.mediscan.mediscan_ai_backend.exception.ResourceNotFoundException;
import com.mediscan.mediscan_ai_backend.repository.MedecinRepository;
import com.mediscan.mediscan_ai_backend.repository.PatientRepository;
import com.mediscan.mediscan_ai_backend.repository.UtilisateurRepository;
import com.mediscan.mediscan_ai_backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Inscription (patient / médecin), connexion et profil de l'utilisateur connecté. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PatientRepository patientRepository;
    private final MedecinRepository medecinRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse inscrirePatient(RegisterPatientRequest req) {
        String email = normaliserEmail(req.email());
        verifierEmailLibre(email);

        Patient patient = new Patient();
        patient.setNom(req.nom().trim());
        patient.setPrenom(req.prenom().trim());
        patient.setEmail(email);
        patient.setMotDePasseHash(passwordEncoder.encode(req.motDePasse()));
        patient.setDateNaissance(req.dateNaissance());
        patient.setSexe(req.sexe());
        patient.setGroupeSanguin(req.groupeSanguin());
        patient.setTelephone(req.telephone());

        Patient enregistre = patientRepository.save(patient);
        log.info("Nouveau patient inscrit : id={}", enregistre.getId());
        return construireReponse(enregistre);
    }

    @Transactional
    public AuthResponse inscrireMedecin(RegisterMedecinRequest req) {
        String email = normaliserEmail(req.email());
        verifierEmailLibre(email);
        if (medecinRepository.existsByNumeroRpps(req.numeroRpps())) {
            throw new ConflitException("Ce numéro RPPS est déjà enregistré");
        }

        Medecin medecin = new Medecin();
        medecin.setNom(req.nom().trim());
        medecin.setPrenom(req.prenom().trim());
        medecin.setEmail(email);
        medecin.setMotDePasseHash(passwordEncoder.encode(req.motDePasse()));
        medecin.setNumeroRpps(req.numeroRpps());
        medecin.setSpecialite(req.specialite().trim());
        medecin.setTelephone(req.telephone());

        Medecin enregistre = medecinRepository.save(medecin);
        log.info("Nouveau médecin inscrit : id={}", enregistre.getId());
        return construireReponse(enregistre);
    }

    /**
     * Connexion : AuthenticationManager vérifie email + mot de passe (BCrypt).
     * En cas d'échec, il lève BadCredentialsException → 401 (GlobalExceptionHandler).
     */
    @Transactional(readOnly = true)
    public AuthResponse connecter(LoginRequest req) {
        String email = normaliserEmail(req.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, req.motDePasse()));

        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        log.info("Connexion réussie : id={}, rôle={}", utilisateur.getId(), utilisateur.getRole());
        return construireReponse(utilisateur);
    }

    /** Profil de l'utilisateur connecté (GET /api/auth/me). */
    @Transactional(readOnly = true)
    public UtilisateurResponse profilConnecte(Long utilisateurId) {
        return utilisateurRepository.findById(utilisateurId)
                .map(UtilisateurResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    // ---------- méthodes utilitaires ----------

    private AuthResponse construireReponse(Utilisateur utilisateur) {
        String token = jwtService.genererToken(utilisateur);
        return AuthResponse.of(token, jwtService.getExpirationMs(), UtilisateurResponse.from(utilisateur));
    }

    private void verifierEmailLibre(String email) {
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitException("Un compte existe déjà avec cet email");
        }
    }

    private String normaliserEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}