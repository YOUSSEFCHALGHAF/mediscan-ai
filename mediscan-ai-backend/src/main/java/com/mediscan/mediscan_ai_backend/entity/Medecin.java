package com.mediscan.mediscan_ai_backend.entity;

import com.mediscan.mediscan_ai_backend.entity.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Médecin : table "medecins", dont l'id est aussi la clé de "users". */
@Entity
@Table(name = "medecins")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
public class Medecin extends Utilisateur {

    /** Numéro professionnel (RPPS) : unique. */
    @Column(name = "numero_rppx", nullable = false, unique = true, length = 20)
    private String numeroRpps;

    @Column(name = "specialite", nullable = false, length = 100)
    private String specialite;

    @Column(name = "telephone", length = 20)
    private String telephone;

    /** Un Medecin a toujours le rôle MEDECIN. */
    public Medecin() {
        super();
        setRole(Role.MEDECIN);
    }
}