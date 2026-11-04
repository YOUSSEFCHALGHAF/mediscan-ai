package com.mediscan.mediscan_ai_backend.service.image;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Opérations sur les images en Java standard (aucune dépendance Spring) :
 * lecture des dimensions sans décoder l'image, et ré-encodage "propre".
 */
public final class TraitementImage {

    private TraitementImage() {
    }

    /** Largeur et hauteur d'une image. */
    public record Dimensions(int largeur, int hauteur) {
    }

    /**
     * Lit uniquement l'en-tête de l'image pour connaître ses dimensions,
     * SANS la décoder : une "bombe" de 30 000 x 30 000 pixels est refusée
     * avant d'occuper plusieurs Go de mémoire.
     *
     * @return les dimensions, ou null si le contenu n'est pas une image lisible
     */
    public static Dimensions lireDimensions(byte[] contenu) {
        try (ImageInputStream flux = ImageIO.createImageInputStream(new ByteArrayInputStream(contenu))) {
            if (flux == null) {
                return null;
            }
            Iterator<ImageReader> lecteurs = ImageIO.getImageReaders(flux);
            if (!lecteurs.hasNext()) {
                return null;
            }
            ImageReader lecteur = lecteurs.next();
            try {
                lecteur.setInput(flux, true, true);
                return new Dimensions(lecteur.getWidth(0), lecteur.getHeight(0));
            } finally {
                lecteur.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * Décode puis encode à nouveau l'image dans son format. Effets  – supprime les métadonnées EXIF (position GPS, modèle du téléphone...)  — Neutralise tout contenu caché à la fin du fichier  – garantit que le fichier stocké est une image valide.
     *
     * @return les octets de la nouvelle image, ou null si l'image est illisible
     */
    public static byte[] reencoder(byte[] contenu, FormatImage format) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(contenu));
            if (image == null) {
                return null;
            }
            // JPEG ne gère pas la transparence : on la remplace par un fond blanc
            BufferedImage aEcrire = format == FormatImage.JPEG ? sansTransparence(image) : image;
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            if (!ImageIO.write(aEcrire, format.nomImageIO(), sortie)) {
                return null;
            }
            return sortie.toByteArray();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static BufferedImage sansTransparence(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB || source.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            return source;
        }
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, source.getWidth(), source.getHeight());
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }
}