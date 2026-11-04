package com.mediscan.mediscan_ai_backend.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test avec le VRAI moteur Tesseract.
 * Exécuté seulement si la variable TESSDATA_PREFIX est définie (installation de l'étape 1) ;
 * ignoré sinon (par exemple sur un serveur d'intégration sans Tesseract).
 */
@EnabledIfEnvironmentVariable(named = "TESSDATA_PREFIX", matches = ".+")
class MoteurOcrTesseractTest {

    @Test
    @DisplayName("Tesseract lit le nom, le lot et la date sur une boîte générée")
    void litUneBoite() {
        BufferedImage boite = new BufferedImage(1400, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = boite.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1400, 600);
        g.setColor(new Color(20, 40, 120));
        g.setFont(new Font("SansSerif", Font.BOLD, 110));
        g.drawString("DOLIPRANE", 80, 200);
        g.setFont(new Font("SansSerif", Font.BOLD, 70));
        g.drawString("1000 mg", 80, 320);
        g.setFont(new Font("Monospaced", Font.PLAIN, 45));
        g.drawString("LOT 24B117   EXP 03/2028", 80, 500);
        g.dispose();

        MoteurOcrTesseract moteur = new MoteurOcrTesseract(System.getenv("TESSDATA_PREFIX"), "fra+eng");
        MoteurOcr.ResultatOcr resultat = moteur.lire(PretraitementImage.niveauxDeGrisContraste(boite));

        assertThat(resultat.texte()).containsIgnoringCase("DOLIPRANE");
        assertThat(resultat.confiance()).isGreaterThan(60);
        var infos = MedicamentTextParser.analyser(resultat.texte());
        assertThat(infos.numeroLot()).isEqualTo("24B117");
    }
}