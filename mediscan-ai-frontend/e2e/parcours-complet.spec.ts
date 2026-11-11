import { expect, test } from '@playwright/test';

import { attendreMessage, COMPTES, lettresAleatoires, photoDeBoite, seConnecter, seDeconnecter } from './outils';

/**
 * PARCOURS COMPLET, dans un vrai navigateur, contre le vrai backend :
 * inscription -> anamnèse (allergie) -> partage avec un médecin -> scan d'un médicament
 * -> alerte d'allergie -> le médecin la voit et la traite -> révocation -> le médecin n'a plus accès.
 *
 * Un nouveau patient est créé à chaque exécution (email unique) : le test peut être relancé.
 */
test('parcours patient et médecin, de l\'inscription à la révocation du consentement', async ({ page }) => {
  const suffixe = lettresAleatoires();
  const patient = {
    prenom: 'Camille',
    nom: `Parcours${suffixe}`,
    email: `e2e.${Date.now()}@mediscan.test`,
    motDePasse: 'Parcours2026!',
  };
  const nomComplet = `${patient.prenom} ${patient.nom}`;
  let urlFicheMedecin = '';

  await test.step('1. Inscription du patient', async () => {
    await page.goto('/inscription');
    await page.getByLabel('Prénom').fill(patient.prenom);
    await page.getByLabel('Nom', { exact: true }).fill(patient.nom);
    await page.getByLabel('Adresse email').fill(patient.email);
    await page.getByLabel('Mot de passe', { exact: true }).fill(patient.motDePasse);
    await page.getByLabel('Confirmation du mot de passe').fill(patient.motDePasse);
    await page.getByRole('checkbox', { name: /J'accepte/ }).check();
    await page.getByRole('button', { name: 'Créer mon compte' }).click();
    await expect(page).toHaveURL(/\/patient\//);
  });

  await test.step('2. Anamnèse : déclarer une allergie à l\'amoxicilline', async () => {
    await page.goto('/patient/anamnese');
    await page.getByRole('tab', { name: /Allergies/ }).click();
    const champ = page.getByRole('combobox', { name: 'Mes allergies' });
    await champ.fill('amoxicilline');
    await champ.press('Enter');
    await expect(page.locator('mat-chip-row').filter({ hasText: 'amoxicilline' })).toBeVisible();

    await page.getByRole('tab', { name: /Récapitulatif/ }).click();
    await page.getByRole('button', { name: 'Enregistrer mon anamnèse' }).click();
    await attendreMessage(page, 'Anamnèse enregistrée');
  });

  await test.step('3. Partage : donner accès au Dr Dubois (anamnèse + alertes)', async () => {
    await page.goto('/patient/partage');
    await page.getByRole('button', { name: 'Donner accès à un médecin' }).click();
    const dialogue = page.getByRole('dialog');
    await dialogue.getByRole('combobox', { name: 'Rechercher un médecin' }).fill('generale');
    await page.getByRole('option', { name: /Dr Claire Dubois/ }).click();
    await dialogue.getByRole('checkbox', { name: /Anamnèse/ }).check();
    await dialogue.getByRole('checkbox', { name: /Alertes médicamenteuses/ }).check();
    await dialogue.getByRole('button', { name: 'Autoriser l\'accès' }).click();
    await attendreMessage(page, 'a désormais accès');
    await expect(page.getByText('Médecins ayant accès (1)')).toBeVisible();
  });

  await test.step('4. Scan d\'une boîte de Clamoxyl : reconnue, avec une alerte d\'allergie', async () => {
    await page.goto('/patient/scanner');
    const photo = await photoDeBoite(page, [
      { texte: 'CLAMOXYL', taille: 110 },
      { texte: '1 g', taille: 70 },
      { texte: 'amoxicilline - 12 comprimés', taille: 48 },
      { texte: 'LOT AB1234   EXP 12/2028', taille: 40 },
    ]);
    await page.locator('input[type="file"][accept="image/jpeg,image/png,image/webp"]')
      .setInputFiles({ name: 'clamoxyl.png', mimeType: 'image/png', buffer: photo });
    await page.getByRole('button', { name: 'Analyser ce médicament' }).click();

    // Analyse OCR + règles : on laisse jusqu'à 30 secondes (le premier appel charge le modèle Tesseract)
    await expect(page.getByText('Médicament reconnu')).toBeVisible({ timeout: 30_000 });
    await expect(page.getByRole('heading', { name: /Clamoxyl/ })).toBeVisible();
    await expect(page.getByText(/alerte\(s\) de sécurité/)).toBeVisible();
    await expect(page.getByText(/Allergie déclarée \(amoxicilline\)/)).toBeVisible();
    await seDeconnecter(page);
  });

  await test.step('5. Le Dr Dubois voit le patient, son alerte, et la traite', async () => {
    await seConnecter(page, COMPTES.drDubois);
    await page.goto('/medecin/patients');
    await page.getByRole('link', { name: `Ouvrir la fiche de ${nomComplet}` }).first().click();
    await expect(page.getByRole('heading', { name: new RegExp(nomComplet) })).toBeVisible();
    urlFicheMedecin = page.url();

    await page.getByRole('tab', { name: /Alertes \(1\)/ }).click();
    await expect(page.getByText(/Allergie déclarée \(amoxicilline\)/)).toBeVisible();
    await page.getByRole('button', { name: 'Marquer comme traitée' }).click();
    await attendreMessage(page, 'Alerte marquée comme traitée');
    await expect(page.getByRole('tab', { name: /Alertes \(0\)/ })).toBeVisible();
    await seDeconnecter(page);
  });

  await test.step('6. Le patient retire l\'accès du Dr Dubois', async () => {
    await seConnecter(page, patient.email, patient.motDePasse);
    await page.goto('/patient/partage');
    await page.getByRole('button', { name: 'Retirer l\'accès' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Retirer l\'accès' }).click();
    await attendreMessage(page, 'Accès du Dr Claire Dubois retiré');
    await expect(page.getByText('Médecins ayant accès (0)')).toBeVisible();
    await expect(page.getByText(/Historique des accès retirés \(1\)/)).toBeVisible();
    await seDeconnecter(page);
  });

  await test.step('7. Révocation immédiate : le Dr Dubois n\'a plus accès', async () => {
    await seConnecter(page, COMPTES.drDubois);
    await page.goto(urlFicheMedecin);
    await expect(page.getByRole('heading', { name: 'Accès non autorisé' })).toBeVisible();

    await page.goto('/medecin/patients');
    await expect(page.getByRole('link', { name: `Ouvrir la fiche de ${nomComplet}` })).toHaveCount(0);
  });
});