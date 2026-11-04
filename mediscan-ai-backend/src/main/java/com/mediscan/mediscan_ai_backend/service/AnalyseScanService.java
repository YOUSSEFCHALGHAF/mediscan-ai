package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.entity.Medicament;
import com.mediscan.mediscan_ai_backend.entity.ScanMedicament;
import com.mediscan.mediscan_ai_backend.entity.enums.StatutScan;
import com.mediscan.mediscan_ai_backend.ia.MedicamentTextParser;
import com.mediscan.mediscan_ai_backend.ia.MoteurOcr;
import com.mediscan.mediscan_ai_backend.ia.PretraitementImage;
import com.mediscan.mediscan_ai_backend.ia.RapprochementMedicament;
import com.mediscan.mediscan_ai_backend.repository.MedicamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Analyse IA d'une photo de médicament :
 * prétraitement → OCR → extraction (nom, dosage, lot, expiration) → rapprochement avec la base → statut.
 * Met à jour l'entité ScanMedicament (la transaction appelante l'enregistre).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyseScanService {

    /** Confiance OCR au-delà de laquelle une correspondance est considérée comme sûre. */
    public static final double CONFIANCE_OCR_SUFFISANTE = 60.0;
    /** Similarité au-delà de laquelle une correspondance est sûre, quelle que soit la confiance OCR. */
    public static final double SIMILARITE_SURE = 0.85;
    private static final int LONGUEUR_MAX_TEXTE = 5000;

    private final MoteurOcr moteurOcr;
    private final MedicamentRepository medicamentRepository;
    private final StockageImageService stockageImage;

    /** Une lecture complète : texte OCR, informations extraites et médicament trouvé. */
    private record Lecture(MoteurOcr.ResultatOcr ocr,
                           MedicamentTextParser.InfosExtraites infos,
                           RapprochementMedicament.Correspondance correspondance) {

        /** Plus la lecture contient d'informations utiles, meilleure, elle est. */
        int qualite() {
            return (correspondance != null ? 2 : 0)
                    + (infos.numeroLot() != null ? 1 : 0)
                    + (infos.dateExpiration() != null ? 1 : 0);
        }

        boolean incomplete() {
            return correspondance == null || (infos.numeroLot() == null && infos.dateExpiration() == null);
        }
    }

    public void analyser(ScanMedicament scan) {
        long debut = System.currentTimeMillis();
        try {
            List<Medicament> base = medicamentRepository.findAll();
            List<RapprochementMedicament.Reference> references = base.stream()
                    .map(m -> new RapprochementMedicament.Reference(m.getId(), m.getNomCommercial(), m.getDci(), m.getDosage()))
                    .toList();

            BufferedImage gris = PretraitementImage.niveauxDeGrisContraste(chargerImage(scan.getImageUrl()));
            Lecture lecture = lire(gris, references);

            // 2e essai en noir et blanc (Otsu) si la première lecture est incomplète
            if (lecture.incomplete()) {
                Lecture seconde = lire(PretraitementImage.binariser(gris), references);
                if (seconde.qualite() > lecture.qualite()) {
                    lecture = seconde;
                }
            }

            Map<Long, Medicament> parId = base.stream().collect(Collectors.toMap(Medicament::getId, Function.identity()));
            appliquer(scan, lecture, parId);

            log.info("Scan {} analysé en {} ms : statut={}, médicament={}, confiance OCR={} %, similarité={}",
                    scan.getId(), System.currentTimeMillis() - debut, scan.getStatut(),
                    scan.getMedicament() != null ? scan.getMedicament().getNomCommercial() : "-",
                    Math.round(lecture.ocr().confiance()),
                    lecture.correspondance() != null ? String.format("%.2f", lecture.correspondance().score()) : "-");
        } catch (IOException | RuntimeException | LinkageError e) {
            // LinkageError : bibliothèque native Tesseract absente ou incompatible
            log.error("Échec de l'analyse du scan {} : {}", scan.getId(), e.toString());
            scan.setStatut(StatutScan.ECHEC);
        }
    }

    private Lecture lire(BufferedImage image, List<RapprochementMedicament.Reference> references) {
        MoteurOcr.ResultatOcr ocr = moteurOcr.lire(image);
        MedicamentTextParser.InfosExtraites infos = MedicamentTextParser.analyser(ocr.texte());
        return new Lecture(ocr, infos, RapprochementMedicament.trouver(infos, references));
    }

    /** Recopie le résultat dans le scan et décide du statut. */
    private void appliquer(ScanMedicament scan, Lecture lecture, Map<Long, Medicament> parId) {
        String texte = lecture.ocr().texte() == null ? "" : lecture.ocr().texte().trim();
        scan.setTexteExtrait(texte.isEmpty() ? null
                : texte.substring(0, Math.min(texte.length(), LONGUEUR_MAX_TEXTE)));
        scan.setScoreConfiance(Math.round(lecture.ocr().confiance() * 10) / 10.0);
        scan.setNumeroLot(lecture.infos().numeroLot());
        scan.setDateExpiration(lecture.infos().dateExpiration());

        // Rien de lisible : échec
        if (texte.chars().filter(Character::isLetter).count() < 3) {
            scan.setMedicament(null);
            scan.setStatut(StatutScan.ECHEC);
            return;
        }

        RapprochementMedicament.Correspondance correspondance = lecture.correspondance();
        if (correspondance == null) {
            scan.setMedicament(null);
            scan.setStatut(StatutScan.A_VERIFIER);
            return;
        }

        scan.setMedicament(parId.get(correspondance.reference().id()));
        boolean certain = correspondance.score() >= SIMILARITE_SURE
                || lecture.ocr().confiance() >= CONFIANCE_OCR_SUFFISANTE;
        scan.setStatut(certain ? StatutScan.TERMINE : StatutScan.A_VERIFIER);
    }

    private BufferedImage chargerImage(String cheminRelatif) throws IOException {
        try (InputStream flux = stockageImage.lire(cheminRelatif).ressource().getInputStream()) {
            BufferedImage image = ImageIO.read(flux);
            if (image == null) {
                throw new IOException("Image illisible : " + cheminRelatif);
            }
            return image;
        }
    }
}