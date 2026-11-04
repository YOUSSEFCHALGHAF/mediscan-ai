import { Service } from '@angular/core';

import { calculerDimensions, DIMENSION_MAX_PX, QUALITE_JPEG } from '../utils/image';

/**
 * Compresse une photo dans le navigateur AVANT l'envoi :
 * - redimensionnement à 1600 px maximum (une photo de 12 Mpx passe de ~4 Mo à ~300 Ko) ;
 * - orientation corrigée (photos de smartphone tenues verticalement) ;
 * - conversion en JPEG ;
 * - suppression des métadonnées EXIF (position GPS, modèle du téléphone) : protection de la vie privée.
 */
@Service()
export class CompressionImageService {
  async compresser(source: Blob): Promise<Blob> {
    // createImageBitmap décode l'image et applique l'orientation EXIF
    const bitmap = await createImageBitmap(source, { imageOrientation: 'from-image' });
    try {
      const { largeur, hauteur } = calculerDimensions(bitmap.width, bitmap.height, DIMENSION_MAX_PX);
      const canvas = document.createElement('canvas');
      canvas.width = largeur;
      canvas.height = hauteur;
      const contexte = canvas.getContext('2d');
      if (!contexte) {
        throw new Error('Canvas indisponible');
      }
      // Fond blanc : une image PNG transparente ne devient pas noire en JPEG
      contexte.fillStyle = '#ffffff';
      contexte.fillRect(0, 0, largeur, hauteur);
      contexte.drawImage(bitmap, 0, 0, largeur, hauteur);

      return await new Promise<Blob>((resoudre, rejeter) =>
        canvas.toBlob(
          (blob) => (blob ? resoudre(blob) : rejeter(new Error('Compression impossible'))),
          'image/jpeg',
          QUALITE_JPEG,
        ),
      );
    } finally {
      bitmap.close();
    }
  }
}