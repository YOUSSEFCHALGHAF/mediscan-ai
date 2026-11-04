package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.exception.RequeteInvalideException;
import com.mediscan.mediscan_ai_backend.exception.ResourceNotFoundException;
import com.mediscan.mediscan_ai_backend.service.image.FormatImage;
import com.mediscan.mediscan_ai_backend.service.image.TraitementImage;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Stockage sécurisé des photos de médicaments sur le disque du serveur.
 * Les fichiers sont dans app.upload.dir (dossier "uploads"), qui n'est JAMAIS servi publiquement :
 * une image n'est lisible que via ScanController, après vérification du propriétaire.
 */
@Slf4j
@Service
public class StockageImageService {

    public static final long TAILLE_MAX_OCTETS = 5L * 1024 * 1024;
    public static final int DIMENSION_MAX_PX = 6000;
    private static final String SOUS_DOSSIER_SCANS = "scans";

    private final Path racine;
    private final Path dossierScans;

    /** Image enregistrée : chemin relatif (stocké en base) et caractéristiques. */
    public record ImageStockee(String cheminRelatif, FormatImage format, int largeur, int hauteur, long taille) {
    }

    /** Image relue depuis le disque, prête à être renvoyée au navigateur. */
    public record ImageLue(Resource ressource, MediaType type) {
    }

    public StockageImageService(@Value("${app.upload.dir}") String dossierUpload) {
        this.racine = Paths.get(dossierUpload).toAbsolutePath().normalize();
        this.dossierScans = racine.resolve(SOUS_DOSSIER_SCANS);
    }

    /** Crée le dossier uploads/scans au démarrage s'il n'existe pas. */
    @PostConstruct
    void initialiser() {
        try {
            Files.createDirectories(dossierScans);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de créer le dossier " + dossierScans, e);
        }
        log.info("Images des scans stockées dans : {}", dossierScans);
    }

    /**
     * Vérifie, nettoie et enregistre une image envoyée par le navigateur.
     * Lᵉve RequeteInvalideException (HTTP 400) si le fichier n'est pas acceptable.
     */
    public ImageStockee enregistrer(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new RequeteInvalideException("Aucune image reçue (champ « image » vide ou absent)");
        }
        if (fichier.getSize() > TAILLE_MAX_OCTETS) {
            throw new RequeteInvalideException("L'image dépasse la taille maximale de 5 Mo");
        }

        byte[] contenu;
        try {
            contenu = fichier.getBytes();
        } catch (IOException e) {
            throw new RequeteInvalideException("Impossible de lire l'image envoyée");
        }

        // 1. Vrai format, d'après les octets (et non l'extension ou le Content-Type annoncé)
        FormatImage format = FormatImage.detecter(contenu).orElseThrow(() ->
                new RequeteInvalideException("Format non supporté : seules les images JPEG et PNG sont acceptées"));

        // 2. Dimensions lues dans l'en-tête, avant tout décodage
        TraitementImage.Dimensions dimensions = TraitementImage.lireDimensions(contenu);
        if (dimensions == null) {
            throw new RequeteInvalideException("Le fichier n'est pas une image valide");
        }
        if (dimensions.largeur() > DIMENSION_MAX_PX || dimensions.hauteur() > DIMENSION_MAX_PX) {
            throw new RequeteInvalideException(
                    "Image trop grande : " + DIMENSION_MAX_PX + " pixels maximum par côté");
        }

        // 3. Ré-encodage : supprime les métadonnées (GPS...) et tout contenu caché
        byte[] imagePropre = TraitementImage.reencoder(contenu, format);
        if (imagePropre == null) {
            throw new RequeteInvalideException("Le fichier n'est pas une image valide");
        }

        // 4. Nom aléatoire : impossible à deviner, aucun risque d'écraser un autre fichier
        String nomFichier = UUID.randomUUID() + "." + format.extension();
        Path destination = dossierScans.resolve(nomFichier).normalize();
        try {
            Files.write(destination, imagePropre, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException("Enregistrement de l'image impossible", e);
        }

        log.debug("Image enregistrée : {} ({}x{}, {} octets)", nomFichier,
                dimensions.largeur(), dimensions.hauteur(), imagePropre.length);
        return new ImageStockee(SOUS_DOSSIER_SCANS + "/" + nomFichier, format,
                dimensions.largeur(), dimensions.hauteur(), imagePropre.length);
    }

    /** Relit une image enregistrée. 404 si elle n'existe pas (ou si le chemin sort du dossier). */
    public ImageLue lire(String cheminRelatif) {
        Path fichier = resoudre(cheminRelatif);
        if (fichier == null || !Files.isRegularFile(fichier)) {
            throw new ResourceNotFoundException("Image introuvable");
        }
        MediaType type = FormatImage.depuisNomFichier(fichier.getFileName().toString())
                .map(f -> MediaType.parseMediaType(f.typeMime()))
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return new ImageLue(new FileSystemResource(fichier), type);
    }

    /** Supprime une image (silencieusement si elle n'existe pas). */
    public void supprimer(String cheminRelatif) {
        Path fichier = resoudre(cheminRelatif);
        if (fichier == null) {
            return;
        }
        try {
            Files.deleteIfExists(fichier);
        } catch (IOException e) {
            log.warn("Suppression impossible de {} : {}", fichier, e.getMessage());
        }
    }

    /**
     * Transforme un chemin relatif en chemin absolu SÛR :
     * refuse tout chemin qui sortirait du dossier d'upload (ex. "../../application.properties").
     */
    private Path resoudre(String cheminRelatif) {
        if (cheminRelatif == null || cheminRelatif.isBlank()) {
            return null;
        }
        try {
            Path chemin = racine.resolve(cheminRelatif).normalize();
            return chemin.startsWith(racine) ? chemin : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }
}