package com.mediscan.mediscan_ai_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Médicament de la base de référence (simulée).
 * "interactions" : DCI qui interagissent, séparées par des virgules.
 */
@Entity
@Table(name = "medicaments")
@Getter
@Setter
@NoArgsConstructor
public class Medicament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_commercial", nullable = false, length = 150)
    private String nomCommercial;

    /** Dénomination Commune Internationale (substance active). */
    @Column(name = "dci", nullable = false, length = 150)
    private String dci;

    @Column(name = "dosage", length = 50)
    private String dosage;

    @Column(name = "forme_galenique", length = 50)
    private String formeGalenique;

    @Column(name = "posologie", length = 255)
    private String posologie;

    @Column(name = "contre_indications", columnDefinition = "TEXT")
    private String contreIndications;

    @Column(name = "interactions", columnDefinition = "TEXT")
    private String interactions;

    @Column(name = "laboratoire", length = 100)
    private String laboratoire;
}