package com.mediscan.mediscan_ai_backend.entity;

import com.mediscan.mediscan_ai_backend.entity.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Classe mère de Patient et Medecin.
 * Héritage JOINED : les champs communs sont dans "users",
 * les champs spécifiques dans "patients" ou "medecins" (même id).
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /** Hash BCrypt du mot de passe : JAMAIS le mot de passe en clair. */
    @Column(name = "mot_de_passe_hash", nullable = false, length = 255)
    private String motDePasseHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void avantCreation() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }

    /** Méthode utilitaire (non persistée) : "Prénom Nom". */
    public String getNomComplet() {
        return prenom + " " + nom;
    }
}