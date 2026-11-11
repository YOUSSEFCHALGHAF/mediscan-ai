import { expect, Page } from '@playwright/test';

/** Mot de passe des comptes de démonstration (étape 3). */
export const MOT_DE_PASSE_DEMO = 'MediScan2026!';

export const COMPTES = {
  marc: 'marc.lefevre@mediscan.test',
  drMansour: 'karim.mansour@mediscan.test',
  drDubois: 'claire.dubois@mediscan.test',
} as const;

/** Connexion par le formulaire, puis attente de l'espace patient ou médecin. */
export async function seConnecter(page: Page, email: string, motDePasse = MOT_DE_PASSE_DEMO): Promise<void> {
  await page.goto('/connexion');
  await page.getByLabel('Adresse email').fill(email);
  await page.getByLabel('Mot de passe', { exact: true }).fill(motDePasse);
  await page.getByRole('button', { name: 'Se connecter' }).click();
  await expect(page).toHaveURL(/\/(patient|medecin)\//);
}

/** Déconnexion par le menu du compte (bouton en haut à droite). */
export async function seDeconnecter(page: Page): Promise<void> {
  await page.locator('button.compte').click();
  await page.getByRole('menuitem', { name: 'Se déconnecter' }).click();
  await expect(page).toHaveURL(/\/connexion/);
}

/** Attend le message vert/rouge (snackbar) contenant ce texte. */
export async function attendreMessage(page: Page, texte: string | RegExp): Promise<void> {
  await expect(page.locator('mat-snack-bar-container').filter({ hasText: texte })).toBeVisible();
}

/** Lettres aléatoires (les noms de personnes ne contiennent pas de chiffres). */
export function lettresAleatoires(longueur = 6): string {
  const alphabet = 'abcdefghijklmnopqrstuvwxyz';
  return Array.from({ length: longueur }, () => alphabet[Math.floor(Math.random() * alphabet.length)]).join('');
}

/**
 * Fabrique, DANS le navigateur, la photo d'une boîte de médicament (PNG).
 * Le texte est net et contrasté : l'OCR Tesseract le lit de façon fiable (vérifié à l'étape 14).
 */
export async function photoDeBoite(page: Page, lignes: { texte: string; taille: number }[]): Promise<Buffer> {
  const base64 = await page.evaluate((contenu) => {
    const canvas = document.createElement('canvas');
    canvas.width = 1400;
    canvas.height = 800;
    const g = canvas.getContext('2d')!;
    g.fillStyle = '#ffffff';
    g.fillRect(0, 0, canvas.width, canvas.height);
    g.fillStyle = '#14286e';
    let y = 60;
    for (const { texte, taille } of contenu) {
      y += taille * 1.4;
      g.font = `bold ${taille}px Arial, Helvetica, sans-serif`;
      g.fillText(texte, 80, y);
    }
    return canvas.toDataURL('image/png').split(',')[1];
  }, lignes);
  return Buffer.from(base64, 'base64');
}