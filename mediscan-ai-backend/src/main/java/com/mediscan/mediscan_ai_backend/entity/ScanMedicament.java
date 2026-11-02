package com.mediscan.mediscan_ai_backend.entity;

import com.mediscan.mediscan_ai_backend.entity.enums.StatutScan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Photo de médicament analysée par l'OCR. Table "scans". */
@Entity
@Table(name = "scans")
@Getter
@Setter
@NoArgsConstructor
public class ScanMedicament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** Médicament reconnu : null si l'OCR n'a rien identifié. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicament_id")
    private Medicament medicament;

    /** Chemin du fichier image sur le serveur (ex. uploads/scans/xxx.jpg). */
    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "date_scan", nullable = false)
    private LocalDateTime dateScan;

    /** Texte brut lu par Tesseract. */
    @Column(name = "texte_extrait", columnDefinition = "TEXT")
    private String texteExtrait;

    @Column(name = "numero_lot", length = 50)
    private String numeroLot;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    /** Confiance de l'OCR, de 0 à 100. */
    @Column(name = "score_confiance")
    private Double scoreConfiance;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutScan statut = StatutScan.EN_COURS;

    @PrePersist
    protected void avantCreation() {
        if (dateScan == null) {
            dateScan = LocalDateTime.now();
        }
    }
}