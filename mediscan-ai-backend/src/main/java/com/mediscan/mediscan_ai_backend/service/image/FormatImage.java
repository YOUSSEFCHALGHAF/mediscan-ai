package com.mediscan.mediscan_ai_backend.service.image;

import java.util.Arrays;
import java.util.Optional;

/**
 * Formats d'image acceptés, reconnus par leur "signature" (premiers octets du fichier)
 * et NON par l'extension ou le type annoncé par le navigateur, qui peuvent être falsifiés.
 */
public enum FormatImage {

    JPEG("jpg", "jpeg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("png", "png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

    private final String extension;
    private final String nomImageIO;
    private final String typeMime;
    private final byte[] signature;

    FormatImage(String extension, String nomImageIO, String typeMime, byte[] signature) {
        this.extension = extension;
        this.nomImageIO = nomImageIO;
        this.typeMime = typeMime;
        this.signature = signature;
    }

    public String extension() {
        return extension;
    }

    public String nomImageIO() {
        return nomImageIO;
    }

    public String typeMime() {
        return typeMime;
    }

    /** Reconnaît le format à partir des premiers octets du fichier. */
    public static Optional<FormatImage> detecter(byte[] contenu) {
        if (contenu == null) {
            return Optional.empty();
        }
        for (FormatImage format : values()) {
            if (contenu.length >= format.signature.length
                    && Arrays.equals(Arrays.copyOf(contenu, format.signature.length), format.signature)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }

    /** Retrouve le format d'un fichier déjà enregistré à partir de son extension. */
    public static Optional<FormatImage> depuisNomFichier(String nomFichier) {
        String nom = nomFichier.toLowerCase();
        for (FormatImage format : values()) {
            if (nom.endsWith("." + format.extension)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }
}