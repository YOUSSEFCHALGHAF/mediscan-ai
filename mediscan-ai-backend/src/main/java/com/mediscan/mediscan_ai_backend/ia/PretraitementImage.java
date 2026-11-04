package com.mediscan.mediscan_ai_backend.ia;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Prépare une photo de boîte de médicament pour l'OCR (Java standard, sans dépendance).
 * Tesseract lit beaucoup mieux un texte  – assez grand (agrandissement des petites images)  – en niveaux de gris, bien contrasté  – noir sur fond blanc (inversion des textes clairs sur fond foncé).
 */
public final class PretraitementImage {

    /** En dessous de cette largeur, l'image est agrandie (les petits caractères sont mal lus). */
    public static final int LARGEUR_MIN = 1500;
    /** Au-delà, l'image est réduite (plus de pixels = OCR plus lents, sans gain). */
    public static final int LARGEUR_MAX = 2500;

    private PretraitementImage() {
    }

    /**
     * Étape 1 : redimensionnement, niveaux de gris, réduction du bruit,
     * correction de l'éclairage (ombres) et étirement du contraste.
     * C'est la version utilisée en premier par l'OCR.
     */
    public static BufferedImage niveauxDeGrisContraste(BufferedImage source) {
        BufferedImage gris = redimensionnerEnGris(source);
        int largeur = gris.getWidth();
        int hauteur = gris.getHeight();
        byte[] pixels = ((DataBufferByte) gris.getRaster().getDataBuffer()).getData();

        // 1. Filtre médian 3x3 : supprime le "grain" de la photo sans flouter les lettres
        filtreMedian(pixels, largeur, hauteur);

        // 2. Texte clair sur fond foncé : on inverse pour obtenir du texte foncé sur fond clair
        if (moyenne(pixels) < 110) {
            inverser(pixels);
        }

        // 3. Correction de l'éclairage : on estime le fond (flou très large) puis on divise
        //    chaque pixel par ce fond. Les ombres et dégradés disparaissent, le texte reste.
        normaliserEclairage(pixels, largeur, hauteur);

        // 4. Étirement du contraste : 1 % les plus sombres → noir, 1 % les plus clairs → blanc
        int[] histogramme = histogramme(pixels);
        int bas = percentile(histogramme, pixels.length, 0.01);
        int haut = percentile(histogramme, pixels.length, 0.99);
        if (haut - bas > 10) {
            double facteur = 255.0 / (haut - bas);
            for (int i = 0; i < pixels.length; i++) {
                int v = (int) Math.round(((pixels[i] & 0xFF) - bas) * facteur);
                pixels[i] = (byte) Math.max(0, Math.min(255, v));
            }
        }
        return gris;
    }

    /**
     * Étape 2 (si la première lecture est peu fiable) : seuillage noir/blanc par la méthode d'Otsu,
     * qui calcule automatiquement le meilleur seuil séparant le texte du fond.
     */
    public static BufferedImage binariser(BufferedImage grisContraste) {
        BufferedImage resultat = new BufferedImage(grisContraste.getWidth(), grisContraste.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        byte[] source = ((DataBufferByte) grisContraste.getRaster().getDataBuffer()).getData();
        byte[] cible = ((DataBufferByte) resultat.getRaster().getDataBuffer()).getData();

        int seuil = seuilOtsu(histogramme(source), source.length);
        int noirs = 0;
        for (int i = 0; i < source.length; i++) {
            boolean noir = (source[i] & 0xFF) <= seuil;
            cible[i] = (byte) (noir ? 0 : 255);
            if (noir) {
                noirs++;
            }
        }
        // Le fond doit être blanc : s'il y a plus de noir que de blanc, on inverse
        if (noirs > source.length / 2) {
            inverser(cible);
        }
        return resultat;
    }

    // ------------------------------------------------------------------

    private static BufferedImage redimensionnerEnGris(BufferedImage source) {
        int largeur = source.getWidth();
        int hauteur = source.getHeight();
        double echelle = 1.0;
        if (largeur < LARGEUR_MIN) {
            echelle = (double) LARGEUR_MIN / largeur;
        } else if (largeur > LARGEUR_MAX) {
            echelle = (double) LARGEUR_MAX / largeur;
        }
        int nouvelleLargeur = (int) Math.round(largeur * echelle);
        int nouvelleHauteur = (int) Math.round(hauteur * echelle);

        // Dessiner l'image couleur dans une image TYPE_BYTE_GRAY convertit en niveaux de gris
        BufferedImage gris = new BufferedImage(nouvelleLargeur, nouvelleHauteur, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gris.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, nouvelleLargeur, nouvelleHauteur, null);
        } finally {
            g.dispose();
        }
        return gris;
    }

    /** Filtre médian 3x3 (les bords de l'image sont laissés tels quels). */
    private static void filtreMedian(byte[] pixels, int largeur, int hauteur) {
        byte[] copie = pixels.clone();
        int[] voisins = new int[9];
        for (int y = 1; y < hauteur - 1; y++) {
            for (int x = 1; x < largeur - 1; x++) {
                int k = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    int ligne = (y + dy) * largeur;
                    for (int dx = -1; dx <= 1; dx++) {
                        voisins[k++] = copie[ligne + x + dx] & 0xFF;
                    }
                }
                java.util.Arrays.sort(voisins);
                pixels[y * largeur + x] = (byte) voisins[4];
            }
        }
    }

    /**
     * Divise chaque pixel par la luminosité moyenne de son voisinage (fenêtre large,
     * plus grande que les lettres). Calcul rapide grâce à une "image intégrale".
     */
    private static void normaliserEclairage(byte[] pixels, int largeur, int hauteur) {
        int rayon = Math.max(15, largeur / 30);
        long[] integrale = new long[(largeur + 1) * (hauteur + 1)];
        for (int y = 0; y < hauteur; y++) {
            long sommeLigne = 0;
            for (int x = 0; x < largeur; x++) {
                sommeLigne += pixels[y * largeur + x] & 0xFF;
                integrale[(y + 1) * (largeur + 1) + (x + 1)] = integrale[y * (largeur + 1) + (x + 1)] + sommeLigne;
            }
        }
        for (int y = 0; y < hauteur; y++) {
            int y0 = Math.max(0, y - rayon);
            int y1 = Math.min(hauteur, y + rayon + 1);
            for (int x = 0; x < largeur; x++) {
                int x0 = Math.max(0, x - rayon);
                int x1 = Math.min(largeur, x + rayon + 1);
                long somme = integrale[y1 * (largeur + 1) + x1] - integrale[y0 * (largeur + 1) + x1]
                        - integrale[y1 * (largeur + 1) + x0] + integrale[y0 * (largeur + 1) + x0];
                double fond = (double) somme / ((long) (x1 - x0) * (y1 - y0));
                int v = (int) Math.round(255.0 * (pixels[y * largeur + x] & 0xFF) / Math.max(fond, 1));
                pixels[y * largeur + x] = (byte) Math.min(255, v);
            }
        }
    }

    private static int[] histogramme(byte[] pixels) {
        int[] h = new int[256];
        for (byte p : pixels) {
            h[p & 0xFF]++;
        }
        return h;
    }

    private static int percentile(int[] histogramme, int total, double fraction) {
        long cible = Math.round(total * fraction);
        long cumul = 0;
        for (int v = 0; v < 256; v++) {
            cumul += histogramme[v];
            if (cumul >= cible) {
                return v;
            }
        }
        return 255;
    }

    private static double moyenne(byte[] pixels) {
        long somme = 0;
        for (byte p : pixels) {
            somme += p & 0xFF;
        }
        return pixels.length == 0 ? 255 : (double) somme / pixels.length;
    }

    private static void inverser(byte[] pixels) {
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (byte) (255 - (pixels[i] & 0xFF));
        }
    }

    /** Méthode d'Otsu : seuil qui maximise la variance entre les classes "texte" et "fond". */
    private static int seuilOtsu(int[] histogramme, int total) {
        double sommeTotale = 0;
        for (int v = 0; v < 256; v++) {
            sommeTotale += (double) v * histogramme[v];
        }
        double sommeFond = 0;
        long poidsFond = 0;
        double meilleureVariance = -1;
        int meilleurSeuil = 127;
        for (int t = 0; t < 256; t++) {
            poidsFond += histogramme[t];
            if (poidsFond == 0) {
                continue;
            }
            long poidsTexte = total - poidsFond;
            if (poidsTexte == 0) {
                break;
            }
            sommeFond += (double) t * histogramme[t];
            double moyenneFond = sommeFond / poidsFond;
            double moyenneTexte = (sommeTotale - sommeFond) / poidsTexte;
            double variance = (double) poidsFond * poidsTexte * Math.pow(moyenneFond - moyenneTexte, 2);
            if (variance > meilleureVariance) {
                meilleureVariance = variance;
                meilleurSeuil = t;
            }
        }
        return meilleurSeuil;
    }
}