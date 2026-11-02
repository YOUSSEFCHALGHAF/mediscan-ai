package com.mediscan.mediscan_ai_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Anamnèse : une seule par patient (relation 1-1).
 * Les listes (allergies, traitements...) sont saisies séparées par des virgules :
 * le moteur de règles IA les découpera pour détecter les interactions.
 */
@Entity
@Table(name = "anamneses")
@Getter
@Setter
@NoArgsConstructor
public class Anamnese {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, unique = true)
    private Patient patient;

    @Column(name = "antecedents", columnDefinition = "TEXT")
    private String antecedents;

    @Column(name = "allergies", columnDefinition = "TEXT")
    private String allergies;

    @Column(name = "pathologies_chroniques", columnDefinition = "TEXT")
    private String pathologiesChroniques;

    @Column(name = "traitements_en_cours", columnDefinition = "TEXT")
    private String traitementsEnCours;

    @Column(name = "habitudes_vie", columnDefinition = "TEXT")
    private String habitudesVie;

    @Column(name = "date_maj", nullable = false)
    private LocalDateTime dateMaj;

    /** Met à jour la date à chaque création ou modification. */
    @PrePersist
    @PreUpdate
    protected void mettreAJourDate() {
        dateMaj = LocalDateTime.now();
    }
}