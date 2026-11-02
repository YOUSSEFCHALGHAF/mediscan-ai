package com.mediscan.mediscan_ai_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Historique des notifications envoyées (aussi affichées dans l'application). */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    /** Types de notification utilisés dans l'application. */
    public static final String TYPE_ALERTE = "ALERTE";
    public static final String TYPE_RAPPEL = "RAPPEL";
    public static final String TYPE_PARTAGE = "PARTAGE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "titre", nullable = false, length = 150)
    private String titre;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi;

    @Column(name = "lue", nullable = false)
    private boolean lue = false;

    @PrePersist
    protected void avantCreation() {
        if (dateEnvoi == null) {
            dateEnvoi = LocalDateTime.now();
        }
    }
}