package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.dto.response.AjoutTraitementResponse;
import com.mediscan.mediscan_ai_backend.dto.response.ScanResponse;
import com.mediscan.mediscan_ai_backend.entity.enums.StatutScan;
import com.mediscan.mediscan_ai_backend.exception.RequeteInvalideException;
import com.mediscan.mediscan_ai_backend.exception.ResourceNotFoundException;
import com.mediscan.mediscan_ai_backend.ia.MoteurOcr;
import com.mediscan.mediscan_ai_backend.repository.ScanMedicamentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Prérequis : database/02_donnees_test.sql exécuté.
 * Le moteur OCR est SIMULÉ (@MockitoBean) : on contrôle exactement le texte "lu".
 * Chaque test est annulé à la fin (@Transactional) : la base reste intacte.
 */
@SpringBootTest(properties = "app.upload.dir=target/uploads-test")
@Transactional
class ScanServiceIntegrationTests {

    private static final long YASMINE = 3L;
    private static final long MARC = 4L;
    /** Données de démo : scan n°2 = Advil de Marc ; scan n°4 = scan flou de Yasmine (A_VERIFIER). */
    private static final long SCAN_ADVIL_MARC = 2L;
    private static final long SCAN_FLOU_YASMINE = 4L;
    private static final long CLAMOXYL = 6L;

    @Autowired private ScanService scanService;
    @Autowired private ScanMedicamentRepository scanRepository;

    @MockitoBean private MoteurOcr moteurOcr;

    private static MockMultipartFile photo() throws IOException {
        BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", sortie);
        return new MockMultipartFile("image", "boite.jpg", "image/jpeg", sortie.toByteArray());
    }

    private void ocrLit(String texte, double confiance) {
        when(moteurOcr.lire(any())).thenReturn(new MoteurOcr.ResultatOcr(texte, confiance));
    }

    // ==================== Analyse (étape 14) ====================

    @Test
    @DisplayName("Boîte bien lue : médicament reconnu, lot et expiration enregistrés, statut TERMINE")
    void medicamentReconnu() throws IOException {
        ocrLit("DOLIPRANE 1000 mg\nparacétamol\nLOT 24B117 EXP 03/2028", 91.0);

        ScanResponse scan = scanService.creerScan(MARC, photo());

        assertThat(scan.statut()).isEqualTo(StatutScan.TERMINE);
        assertThat(scan.medicament().nomCommercial()).isEqualTo("Doliprane");
        assertThat(scan.numeroLot()).isEqualTo("24B117");
        assertThat(scan.dateExpiration()).isEqualTo(LocalDate.of(2028, 3, 31));
        assertThat(scan.scoreConfiance()).isEqualTo(91.0);
        assertThat(scan.texteExtrait()).contains("DOLIPRANE");
    }

    @Test
    @DisplayName("Lecture douteuse : médicament proposé mais statut A_VERIFIER")
    void lectureDouteuse() throws IOException {
        ocrLit("DOLPRXNE", 40.0);

        ScanResponse scan = scanService.creerScan(MARC, photo());

        assertThat(scan.statut()).isEqualTo(StatutScan.A_VERIFIER);
        assertThat(scan.medicament().nomCommercial()).isEqualTo("Doliprane");
    }

    @Test
    @DisplayName("Texte lu mais aucun médicament reconnu : A_VERIFIER sans médicament")
    void aucunMedicament() throws IOException {
        ocrLit("xqzv blrt 12", 35.0);

        ScanResponse scan = scanService.creerScan(MARC, photo());

        assertThat(scan.statut()).isEqualTo(StatutScan.A_VERIFIER);
        assertThat(scan.medicament()).isNull();
    }

    @Test
    @DisplayName("Aucun texte ou erreur du moteur OCR : statut ECHEC")
    void echec() throws IOException {
        ocrLit("", 0);
        assertThat(scanService.creerScan(MARC, photo()).statut()).isEqualTo(StatutScan.ECHEC);

        when(moteurOcr.lire(any())).thenThrow(new IllegalStateException("tessdata introuvable"));
        assertThat(scanService.creerScan(MARC, photo()).statut()).isEqualTo(StatutScan.ECHEC);
    }

    @Test
    @DisplayName("Le propriétaire peut relire l'image ; un autre patient obtient 404")
    void accesImage() throws IOException {
        ocrLit("DOLIPRANE", 90.0);
        ScanResponse scan = scanService.creerScan(MARC, photo());

        assertThat(scanService.image(MARC, scan.id()).type().toString()).isEqualTo("image/jpeg");
        assertThatThrownBy(() -> scanService.image(YASMINE, scan.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Image de démonstration absente du disque : 404 propre")
    void imageDeDemoAbsente() {
        assertThatThrownBy(() -> scanService.image(MARC, SCAN_ADVIL_MARC))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== Correction manuelle (étape 15) ====================

    @Test
    @DisplayName("Corriger un scan à vérifier : le médicament est enregistré et le statut passe à TERMINE")
    void correction() {
        ScanResponse scan = scanService.corrigerMedicament(YASMINE, SCAN_FLOU_YASMINE, CLAMOXYL);

        assertThat(scan.statut()).isEqualTo(StatutScan.TERMINE);
        assertThat(scan.medicament().nomCommercial()).isEqualTo("Clamoxyl");
        assertThat(scanRepository.findById(SCAN_FLOU_YASMINE).orElseThrow().getMedicament().getId())
                .isEqualTo(CLAMOXYL);
    }

    @Test
    @DisplayName("Correction refusée sur le scan d'un autre patient, ou avec un médicament inconnu")
    void correctionRefusee() {
        assertThatThrownBy(() -> scanService.corrigerMedicament(MARC, SCAN_FLOU_YASMINE, CLAMOXYL))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> scanService.corrigerMedicament(YASMINE, SCAN_FLOU_YASMINE, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Médicament");
    }

    // ==================== Ajout aux traitements (étape 15) ====================

    @Test
    @DisplayName("Ajoute la DCI aux traitements en cours, une seule fois")
    void ajoutAuxTraitements() {
        AjoutTraitementResponse premier = scanService.ajouterAuxTraitements(MARC, SCAN_ADVIL_MARC);

        assertThat(premier.substance()).isEqualTo("ibuprofène");
        assertThat(premier.dejaPresent()).isFalse();
        assertThat(premier.anamnese().traitementsEnCours()).isEqualTo("warfarine, amlodipine, ibuprofène");

        AjoutTraitementResponse second = scanService.ajouterAuxTraitements(MARC, SCAN_ADVIL_MARC);
        assertThat(second.dejaPresent()).isTrue();
        assertThat(second.anamnese().traitementsEnCours()).isEqualTo("warfarine, amlodipine, ibuprofène");
    }

    @Test
    @DisplayName("Impossible d'ajouter aux traitements un scan sans médicament identifié")
    void ajoutSansMedicament() {
        assertThatThrownBy(() -> scanService.ajouterAuxTraitements(YASMINE, SCAN_FLOU_YASMINE))
                .isInstanceOf(RequeteInvalideException.class);
    }

    // ==================== Suppression (étape 15) ====================

    @Test
    @DisplayName("Supprime le scan du patient")
    void suppression() throws IOException {
        ocrLit("DOLIPRANE", 90.0);
        ScanResponse scan = scanService.creerScan(MARC, photo());

        scanService.supprimer(MARC, scan.id());

        assertThat(scanRepository.findById(scan.id())).isEmpty();
    }

    @Test
    @DisplayName("Un patient ne peut pas supprimer le scan d'un autre")
    void suppressionRefusee() {
        assertThatThrownBy(() -> scanService.supprimer(YASMINE, SCAN_ADVIL_MARC))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(scanRepository.findById(SCAN_ADVIL_MARC)).isPresent();
    }
}