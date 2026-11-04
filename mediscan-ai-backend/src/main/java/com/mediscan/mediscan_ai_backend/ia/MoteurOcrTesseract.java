package com.mediscan.mediscan_ai_backend.ia;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * OCR avec Tesseract (via la bibliothèque Tess4J), 100 % local et gratuit.
 * Configuration : app.ocr.tessdata-path (dossier contenant fra.traineddata) et app.ocr.langage.
 */
@Slf4j
@Component
public class MoteurOcrTesseract implements MoteurOcr {

    private final String dossierTessdata;
    private final String langues;

    public MoteurOcrTesseract(@Value("${app.ocr.tessdata-path}") String dossierTessdata,
                              @Value("${app.ocr.language:fra+eng}") String langues) {
        this.dossierTessdata = dossierTessdata;
        this.langues = langues;
    }

    /** Vérifie au démarrage que les fichiers de langue existent (sinon : avertissement clair). */
    @PostConstruct
    void verifierInstallation() {
        for (String langue : langues.split("\\+")) {
            Path fichier = Paths.get(dossierTessdata, langue + ".traineddata");
            if (!Files.isRegularFile(fichier)) {
                log.warn("OCR : fichier de langue introuvable {} - les scans passeront en ECHEC. "
                        + "Vérifiez app.ocr.tessdata-path (installation de Tesseract, étape 1).", fichier);
            }
        }
        log.info("OCR Tesseract configuré : langues={}, données={}", langues, dossierTessdata);
    }

    @Override
    public ResultatOcr lire(BufferedImage image) {
        // Une instance par analyse : Tesseract n'est pas utilisable par plusieurs requêtes à la fois
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(dossierTessdata);
        tesseract.setLanguage(langues);
        tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_AUTO);
        tesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);
        tesseract.setVariable("user_defined_dpi", "300");

        // Lecture LIGNE par LIGNE : on obtient à la fois le texte et la confiance de chaque ligne
        List<Word> lignes = tesseract.getWords(List.of(image), ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);

        StringBuilder texte = new StringBuilder();
        double sommePonderee = 0;
        int nombreCaracteres = 0;
        for (Word ligne : lignes) {
            String contenu = ligne.getText() == null ? "" : ligne.getText().trim();
            if (contenu.isEmpty()) {
                continue;
            }
            texte.append(contenu).append('\n');
            // Confiance moyenne pondérée par la longueur : une longue ligne compte plus qu'un caractère isolé
            sommePonderee += ligne.getConfidence() * contenu.length();
            nombreCaracteres += contenu.length();
        }
        double confiance = nombreCaracteres == 0 ? 0 : sommePonderee / nombreCaracteres;
        return new ResultatOcr(texte.toString().trim(), Math.max(0, Math.min(100, confiance)));
    }
}