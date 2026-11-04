package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.dto.response.ScanResponse;
import com.mediscan.mediscan_ai_backend.entity.enums.StatutScan;
import com.mediscan.mediscan_ai_backend.exception.ResourceNotFoundException;
import com.mediscan.mediscan_ai_backend.ia.MoteurOcr;
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
 * Le moteur OCR est SIMULÉ (@MockitoBean) : on contrôle exactement le texte "lu",
 * ce qui permet de tester toute la chaîne (analyse, base, statut) sans dépendre de Tesseract.
 */
@SpringBootTest(properties = "app.upload.dir=target/uploads-test")
@Transactional
class ScanServiceIntegrationTests {

    private static final long YASMINE = 3L;
    private static final long MARC = 4L;

    @Autowired private ScanService scanService;

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
        ocrLit("DOLPRXNE", 40.0);   // similarité 0,78 et confiance faible

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
        assertThatThrownBy(() -> scanService.image(MARC, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}