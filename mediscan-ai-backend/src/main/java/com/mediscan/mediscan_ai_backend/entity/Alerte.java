package com.mediscan.mediscan_ai_backend.entity;


import com.mediscan.mediscan_ai_backend.entity.enums.NiveauGravite;
import com.mediscan.mediscan_ai_backend.entity.enums.TypeAlerte;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Alerte générée par le moteur de règles (interaction, allergie...). */
@Entity
@Table(name = "alertes")
@Getter
@Setter
@NoArgsConstructor
public class Alerte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** Scan à l'origine de l'alerte (peut être null). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id")
    private ScanMedicament scan;

    /** Médicament concerné (peut être null). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicament_id")
    private Medicament medicament;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TypeAlerte type;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau_gravite", nullable = false)
    private NiveauGravite niveauGravite;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "date_generation", nullable = false)
    private LocalDateTime dateGeneration;

    @Column(name = "traitee", nullable = false)
    private boolean traitee = false;

    @PrePersist
    protected void avantCreation() {
        if (dateGeneration == null) {
            dateGeneration = LocalDateTime.now();
        }
    }

    /** Le médecin a pris connaissance de l'alerte. */
    public void marquerTraitee() {
        this.traitee = true;
    }
}