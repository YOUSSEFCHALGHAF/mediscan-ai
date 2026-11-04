package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.exception.RequeteInvalideException;
import com.mediscan.mediscan_ai_backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Test unitaire : un dossier temporaire remplace "uploads". */
class StockageImageServiceTest {

    @TempDir
    Path dossier;

    private StockageImageService service;

    @BeforeEach
    void preparer() {
        service = new StockageImageService(dossier.toString());
        service.initialiser();
    }

    private static byte[] imagePng(int largeur, int hauteur) throws IOException {
        BufferedImage image = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        ImageIO.write(image, "png", sortie);
        return sortie.toByteArray();
    }

    @Test
    @DisplayName("Enregistre une image valide sous un nom aléatoire dans scans/")
    void enregistreUneImageValide() throws IOException {
        var fichier = new MockMultipartFile("image", "boite.png", "image/png", imagePng(800, 600));

        var stockee = service.enregistrer(fichier);

        assertThat(stockee.cheminRelatif()).startsWith("scans/").endsWith(".png");
        assertThat(stockee.cheminRelatif()).doesNotContain("boite");
        assertThat(stockee.largeur()).isEqualTo(800);
        assertThat(Files.exists(dossier.resolve(stockee.cheminRelatif()))).isTrue();
    }

    @Test
    @DisplayName("Refuse un fichier texte déguisé en image (.jpg + image/jpeg)")
    void refuseUnTexteDeguiseEnImage() {
        var faux = new MockMultipartFile("image", "boite.jpg", "image/jpeg",
                "Ceci n'est pas une image".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.enregistrer(faux))
                .isInstanceOf(RequeteInvalideException.class)
                .hasMessageContaining("Format non supporté");
    }

    @Test
    @DisplayName("Refuse un fichier vide")
    void refuseUnFichierVide() {
        var vide = new MockMultipartFile("image", "vide.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.enregistrer(vide))
                .isInstanceOf(RequeteInvalideException.class)
                .hasMessageContaining("Aucune image");
    }

    @Test
    @DisplayName("Refuse une image de plus de 6000 pixels de côté")
    void refuseUneImageTropGrande() throws IOException {
        var geante = new MockMultipartFile("image", "geante.png", "image/png", imagePng(6001, 10));

        assertThatThrownBy(() -> service.enregistrer(geante))
                .isInstanceOf(RequeteInvalideException.class)
                .hasMessageContaining("trop grande");
    }

    @Test
    @DisplayName("Refuse de lire un fichier hors du dossier d'upload (path traversal)")
    void refuseDeSortirDuDossier() {
        assertThatThrownBy(() -> service.lire("../../application.properties"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Relit puis supprime une image")
    void relitPuisSupprime() throws IOException {
        var stockee = service.enregistrer(
                new MockMultipartFile("image", "b.png", "image/png", imagePng(100, 100)));

        assertThat(service.lire(stockee.cheminRelatif()).type().toString()).isEqualTo("image/png");

        service.supprimer(stockee.cheminRelatif());
        assertThat(Files.exists(dossier.resolve(stockee.cheminRelatif()))).isFalse();
    }
}