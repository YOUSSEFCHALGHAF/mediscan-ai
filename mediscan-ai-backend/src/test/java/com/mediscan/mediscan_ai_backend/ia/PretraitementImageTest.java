package com.mediscan.mediscan_ai_backend.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class PretraitementImageTest {

    private static BufferedImage boite(Color fond, Color texte, int largeur, int hauteur) {
        BufferedImage image = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(fond);
        g.fillRect(0, 0, largeur, hauteur);
        g.setColor(texte);
        g.setFont(new Font("SansSerif", Font.BOLD, hauteur / 6));
        g.drawString("DOLIPRANE", largeur / 10, hauteur / 2);
        g.dispose();
        return image;
    }

    private static int gris(BufferedImage image, int x, int y) {
        return image.getRaster().getSample(x, y, 0);
    }

    @Test
    @DisplayName("Inverse un texte clair sur fond foncé (le fond devient clair)")
    void inversion() {
        BufferedImage resultat = PretraitementImage.niveauxDeGrisContraste(
                boite(new Color(10, 60, 160), Color.WHITE, 1600, 800));

        assertThat(gris(resultat, 20, 20)).isGreaterThan(200);
    }

    @Test
    @DisplayName("Efface une ombre : le fond devient clair des deux côtés")
    void correctionOmbre() {
        BufferedImage image = boite(Color.WHITE, Color.BLACK, 1600, 800);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                double lumiere = 0.25 + 0.75 * x / 1600.0;   // côté gauche dans l'ombre
                int v = (int) ((image.getRGB(x, y) & 0xFF) * lumiere);
                image.setRGB(x, y, (v << 16) | (v << 8) | v);
            }
        }
        assertThat(image.getRGB(20, 20) & 0xFF).isLessThan(100);   // avant : fond sombre à gauche

        BufferedImage resultat = PretraitementImage.niveauxDeGrisContraste(image);

        assertThat(gris(resultat, 20, 20)).isGreaterThan(200);
        assertThat(gris(resultat, 1580, 20)).isGreaterThan(200);
    }

    @Test
    @DisplayName("Agrandit une petite image à 1500 px et la convertit en niveaux de gris")
    void agrandissement() {
        BufferedImage resultat = PretraitementImage.niveauxDeGrisContraste(boite(Color.WHITE, Color.BLACK, 700, 350));

        assertThat(resultat.getWidth()).isEqualTo(PretraitementImage.LARGEUR_MIN);
        assertThat(resultat.getType()).isEqualTo(BufferedImage.TYPE_BYTE_GRAY);
    }

    @Test
    @DisplayName("La binarisation ne produit que du noir et du blanc, fond blanc")
    void binarisation() {
        BufferedImage gris = PretraitementImage.niveauxDeGrisContraste(boite(Color.WHITE, Color.BLACK, 700, 350));
        BufferedImage resultat = PretraitementImage.binariser(gris);

        for (int y = 0; y < resultat.getHeight(); y += 7) {
            for (int x = 0; x < resultat.getWidth(); x += 7) {
                assertThat(gris(resultat, x, y)).isIn(0, 255);
            }
        }
        assertThat(gris(resultat, 5, 5)).isEqualTo(255);
    }
}