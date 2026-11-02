package com.mediscan.mediscan_ai_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Rappel de prise médicamenteuse (notifié par Firebase à l'étape 17). */
@Entity
@Table(name = "rappels")
@Getter
@Setter
@NoArgsConstructor
public class Rappel {

    /** Valeurs autorisées par la contrainte CHECK chk_rappels_frequence. */
    public static final String QUOTIDIEN = "QUOTIDIEN";
    public static final String HEBDOMADAIRE = "HEBDOMADAIRE";
    public static final String PONCTUEL = "PONCTUEL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicament_id", nullable = false)
    private Medicament medicament;

    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    @Column(name = "heure_prise", nullable = false)
    private LocalTime heurePrise;

    @Column(name = "frequence", nullable = false, length = 20)
    private String frequence = QUOTIDIEN;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    /** Évite d'envoyer deux fois le même rappel (utilisé par le @Scheduled). */
    @Column(name = "derniere_notification")
    private LocalDateTime derniereNotification;

    public void activer() {
        this.actif = true;
    }

    public void desactiver() {
        this.actif = false;
    }
}