package com.mediscan.mediscan_ai_backend.repository;


import com.mediscan.mediscan_ai_backend.entity.*;
import com.mediscan.mediscan_ai_backend.entity.enums.Role;
import com.mediscan.mediscan_ai_backend.entity.enums.StatutScan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration sur la VRAIE base mediscan_db.
 * Prérequis : avoir exécuté database/02_donnees_test.sql.
 * @Transactional → chaque test est annulé (rollback) à la fin : la base n'est pas modifiée.
 */
@SpringBootTest
@Transactional
class RepositoryIntegrationTests {

    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private MedecinRepository medecinRepository;
    @Autowired private AnamneseRepository anamneseRepository;
    @Autowired private MedicamentRepository medicamentRepository;
    @Autowired private ScanMedicamentRepository scanRepository;
    @Autowired private AlerteRepository alerteRepository;
    @Autowired private RappelRepository rappelRepository;
    @Autowired private PartageDonneesRepository partageRepository;
    @Autowired private NotificationRepository notificationRepository;

    @Test
    @DisplayName("Recherche par email : renvoie le bon type (Patient) grâce à l'héritage JOINED")
    void rechercheUtilisateurPolymorphe() {
        Utilisateur u = utilisateurRepository
                .findByEmailIgnoreCase("MARC.LEFEVRE@mediscan.test").orElseThrow();

        assertThat(u).isInstanceOf(Patient.class);
        assertThat(u.getRole()).isEqualTo(Role.PATIENT);
        assertThat(u.getNomComplet()).isEqualTo("Marc Lefèvre");
    }

    @Test
    @DisplayName("Un médecin est bien chargé comme Medecin avec ses champs propres")
    void rechercheMedecin() {
        Medecin m = medecinRepository
                .findByEmailIgnoreCase("karim.mansour@mediscan.test").orElseThrow();

        assertThat(m.getSpecialite()).isEqualTo("Cardiologie");
        assertThat(m.getRole()).isEqualTo(Role.MEDECIN);
    }

    @Test
    @DisplayName("Consentement : le Dr Mansour voit Marc mais PAS Nadia (révoqué)")
    void patientsAvecConsentementActif() {
        List<Patient> patients = patientRepository.findPatientsAvecConsentementActif(1L);

        assertThat(patients)
                .extracting(Patient::getNom)
                .containsExactly("Lefèvre");
    }

    @Test
    @DisplayName("Vérification d'accès médecin -> patient")
    void verificationAcces() {
        assertThat(partageRepository.existsByPatientIdAndMedecinIdAndActifTrue(4L, 1L)).isTrue();
        assertThat(partageRepository.existsByPatientIdAndMedecinIdAndActifTrue(5L, 1L)).isFalse();
    }

    @Test
    @DisplayName("Anamnèse de Marc : traitement par warfarine")
    void anamnese() {
        Anamnese a = anamneseRepository.findByPatientId(4L).orElseThrow();

        assertThat(a.getTraitementsEnCours()).contains("warfarine");
    }

    @Test
    @DisplayName("Recherche de médicament insensible aux accents (OCR : IBUPROFENE)")
    void rechercheMedicamentSansAccent() {
        List<Medicament> resultats = medicamentRepository.rechercher("IBUPROFENE");

        assertThat(resultats)
                .extracting(Medicament::getNomCommercial)
                .containsExactly("Advil");
    }

    @Test
    @DisplayName("Historique des scans de Yasmine : le plus récent en premier")
    void historiqueScans() {
        List<ScanMedicament> scans = scanRepository.findByPatientIdOrderByDateScanDesc(3L);

        assertThat(scans).hasSize(2);
        assertThat(scans.get(0).getStatut()).isEqualTo(StatutScan.A_VERIFIER);
        assertThat(scans.get(1).getMedicament().getNomCommercial()).isEqualTo("Doliprane");
    }

    @Test
    @DisplayName("Dashboard : 2 alertes non traitées pour le Dr Mansour, 0 pour le Dr Dubois")
    void alertesNonTraiteesPourMedecin() {
        assertThat(alerteRepository.findAlertesNonTraiteesPourMedecin(1L)).hasSize(2);
        assertThat(alerteRepository.countAlertesNonTraiteesPourMedecin(2L)).isZero();
    }

    @Test
    @DisplayName("Rappels à déclencher à 20:00")
    void rappelsADeclencher() {
        List<Rappel> rappels = rappelRepository
                .findRappelsADeclencher(LocalTime.of(20, 0), LocalDate.of(2026, 11, 2));

        assertThat(rappels)
                .extracting(Rappel::getLibelle)
                .containsExactlyInAnyOrder("Coumadine du soir", "Glucophage dîner");
    }

    @Test
    @DisplayName("Notifications non lues de Marc")
    void notificationsNonLues() {
        assertThat(notificationRepository.countByUtilisateurIdAndLueFalse(4L)).isEqualTo(1);
    }

    @Test
    @DisplayName("Enregistrement d'un nouveau patient (annulé à la fin du test)")
    void enregistrementNouveauPatient() {
        Patient p = new Patient();
        p.setNom("Test");
        p.setPrenom("Patient");
        p.setEmail("patient.test@mediscan.test");
        p.setMotDePasseHash("$2a$10$hashFictifPourLeTest");
        p.setSexe("FEMME");
        p.setGroupeSanguin("AB+");
        p.setDateNaissance(LocalDate.of(2000, 1, 1));

        Patient enregistre = patientRepository.saveAndFlush(p);

        assertThat(enregistre.getId()).isNotNull();
        assertThat(enregistre.getRole()).isEqualTo(Role.PATIENT);
        assertThat(enregistre.getDateCreation()).isNotNull();
        assertThat(enregistre.isActif()).isTrue();
    }
}