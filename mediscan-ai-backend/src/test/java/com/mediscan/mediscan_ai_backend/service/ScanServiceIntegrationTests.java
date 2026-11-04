package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.dto.response.ScanResponse;
import com.mediscan.mediscan_ai_backend.entity.enums.StatutScan;
import com.mediscan.mediscan_ai_backend.exception.ResourceNotFoundException;
import com.mediscan.mediscan_ai_backend.repository.ScanMedicamentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prérequis : database/02_donnees_test.sql exécuté.
 * Les images de test sont écrites dans target/uploads-test (effacé par "mvn clean").
 */
@SpringBootTest(properties = "app.upload.dir=target/uploads-test")
@Transactional
class ScanServiceIntegrationTests {

    private static final long YASMINE = 3L;
    private static final long MARC = 4L;

    @Autowired private ScanService scanService;
    @Autowired private ScanMedicamentRepository scanRepository;

    private static MockMultipartFile photo() throws IOException {
        BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", sortie);
        return new MockMultipartFile("image", "boite.jpg", "image/jpeg", sortie.toByteArray());
    }

    @Test
    @DisplayName("Crée un scan EN_COURS avec l'image stockée")
    void creeUnScanEnCours() throws IOException {
        ScanResponse scan = scanService.creerScan(MARC, photo());

        assertThat(scan.statut()).isEqualTo(StatutScan.EN_COURS);
        assertThat(scan.imageUrl()).startsWith("scans/").endsWith(".jpg");
        assertThat(scan.medicament()).isNull();
        assertThat(scanRepository.countByPatientId(MARC)).isEqualTo(2);
    }

    @Test
    @DisplayName("Le propriétaire peut relire l'image de son scan")
    void proprietaireLitSonImage() throws IOException {
        ScanResponse scan = scanService.creerScan(MARC, photo());

        var image = scanService.image(MARC, scan.id());

        assertThat(image.type().toString()).isEqualTo("image/jpeg");
        assertThat(image.ressource().exists()).isTrue();
    }

    @Test
    @DisplayName("Un autre patient ne peut pas lire l'image (404)")
    void autrePatientRefuse() throws IOException {
        ScanResponse scanDeMarc = scanService.creerScan(MARC, photo());

        assertThatThrownBy(() -> scanService.image(YASMINE, scanDeMarc.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Image de démonstration absente du disque : 404 propre")
    void imageDeDemoAbsente() {
        // Le scan n°2 (Advil de Marc) existe en base, mais sa photo n'existe pas sur le disque
        assertThatThrownBy(() -> scanService.image(MARC, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}