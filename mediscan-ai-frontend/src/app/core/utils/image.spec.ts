import { calculerDimensions, formaterTaille, verifierFichierImage } from './image';

describe('Utilitaires image', () => {
  it('réduit une photo de smartphone à 1600 px en gardant les proportions', () => {
    expect(calculerDimensions(4000, 3000)).toEqual({ largeur: 1600, hauteur: 1200 });
    expect(calculerDimensions(3000, 4000)).toEqual({ largeur: 1200, hauteur: 1600 });
  });

  it('n\'agrandit jamais une petite image', () => {
    expect(calculerDimensions(800, 600)).toEqual({ largeur: 800, hauteur: 600 });
  });

  it('refuse un fichier qui n\'est pas une image', () => {
    const pdf = new File(['x'], 'ordonnance.pdf', { type: 'application/pdf' });
    expect(verifierFichierImage(pdf)).toContain('Format non pris en charge');
  });

  it('accepte une photo JPEG', () => {
    const photo = new File(['x'], 'boite.jpg', { type: 'image/jpeg' });
    expect(verifierFichierImage(photo)).toBeNull();
  });

  it('formate une taille lisible', () => {
    expect(formaterTaille(512 * 1024)).toBe('512 Ko');
    expect(formaterTaille(1.5 * 1024 * 1024)).toBe('1,5 Mo');
  });
});