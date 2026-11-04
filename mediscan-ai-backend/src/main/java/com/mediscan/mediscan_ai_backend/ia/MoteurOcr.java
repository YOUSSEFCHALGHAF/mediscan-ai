package com.mediscan.mediscan_ai_backend.ia;

import java.awt.image.BufferedImage;

/**
 * Moteur de reconnaissance de texte (OCR).
 * Interface : l'implémentation réelle utilise Tesseract ; les tests utilisent une version simulée.
 */
public interface MoteurOcr {

    /** Texte lu (une ligne par ligne détectée) et confiance moyenne de 0 à 100. */
    record ResultatOcr(String texte, double confiance) {
    }

    ResultatOcr lire(BufferedImage image);
}