package com.mediscan.mediscan_ai_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Partage des données d'un patient avec un médecin = CONSENTEMENT explicite.
 * Une révocation ne supprime pas la ligne (traçabilité RGPD) :
 * actif passe à false et date_revocation est renseignée.
 */
@Entity
@Table(name = "partages")
@Getter
@Setter
@NoArgsConstructor
public class PartageDonnees {

    /** Valeurs possibles dans perimetre_donnees (séparées par des virgules). */
    public static final String ANAMNESE = "ANAMNESE";
    public static final String SCANS = "SCANS";
    public static final String ALERTES = "ALERTES";
    public static final String RAPPELS = "RAPPELS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medecin_id", nullable = false)
    private Medecin medecin;

    /** Ex. "ANAMNESE,SCANS,ALERTES". */
    @Column(name = "perimetre_donnees", nullable = false, length = 255)
    private String perimetreDonnees;

    @Column(name = "date_consentement", nullable = false, updatable = false)
    private LocalDateTime dateConsentement;

    @Column(name = "date_revocation")
    private LocalDateTime dateRevocation;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @PrePersist
    protected void avantCreation() {
        if (dateConsentement == null) {
            dateConsentement = LocalDateTime.now();
        }
    }

    /** Le patient retire son consentement (respecte chk_partages_coherence). */
    public void revoquer() {
        this.actif = false;
        this.dateRevocation = LocalDateTime.now();
    }

    /** Vérifie si une catégorie de données fait partie du périmètre partagé. */
    public boolean couvre(String categorie) {
        if (perimetreDonnees == null) {
            return false;
        }
        for (String element : perimetreDonnees.split(",")) {
            if (element.trim().equalsIgnoreCase(categorie)) {
                return true;
            }
        }
        return false;
    }
}