package com.mediscan.mediscan_ai_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Token Firebase Cloud Messaging d'un navigateur (un utilisateur peut en avoir plusieurs). */
@Entity
@Table(name = "device_tokens")
@Getter
@Setter
@NoArgsConstructor
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    /** Ex. "Chrome 142 / Windows". */
    @Column(name = "navigateur", length = 100)
    private String navigateur;

    @Column(name = "date_enregistrement", nullable = false)
    private LocalDateTime dateEnregistrement;

    @PrePersist
    protected void avantCreation() {
        if (dateEnregistrement == null) {
            dateEnregistrement = LocalDateTime.now();
        }
    }
}