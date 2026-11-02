package com.mediscan.mediscan_ai_backend.entity;

import com.mediscan.mediscan_ai_backend.entity.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Patient : table "patients", dont l'id est aussi la clé de "users". */
@Entity
@Table(name = "patients")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
public class Patient extends Utilisateur {

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    /** HOMME, FEMME ou AUTRE (contrôlé par la contrainte CHECK en base). */
    @Column(name = "sexe", length = 10)
    private String sexe;

    /** A+, A-, B+, B-, AB+, AB-, O+ ou O-. */
    @Column(name = "groupe_sanguin", length = 3)
    private String groupeSanguin;

    @Column(name = "telephone", length = 20)
    private String telephone;

    /** Un Patient a toujours le rôle PATIENT. */
    public Patient() {
        super();
        setRole(Role.PATIENT);
    }
}