import AxeBuilder from '@axe-core/playwright';
import { expect, Page, test, TestInfo } from '@playwright/test';

import { COMPTES, seConnecter } from './outils';

/**
 * Audit d'accessibilité automatique (axe-core, règles WCAG 2.1 niveaux A et AA).
 * Le test échoue en cas de violation "critique" ou "sérieuse" ; les autres sont jointes au rapport.
 * Un audit automatique détecte environ un tiers des problèmes : il complète, sans la remplacer,
 * une vérification manuelle (clavier, lecteur d'écran).
 */
async function auditer(page: Page, testInfo: TestInfo, nom: string): Promise<void> {
  await page.waitForLoadState('networkidle');
  const resultat = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze();

  await testInfo.attach(`axe-${nom}.json`, { body: JSON.stringify(resultat.violations, null, 2), contentType: 'application/json' });

  const bloquantes = resultat.violations
    .filter((v) => v.impact === 'critical' || v.impact === 'serious')
    .map((v) => `${v.id} (${v.impact}) : ${v.help} -> ${v.nodes.map((n) => `${n.target.join(' ')} | ${n.html} | ${n.failureSummary}`).slice(0, 3).join(' || ')}`);
  expect(bloquantes, `Violations bloquantes sur « ${nom} »`).toEqual([]);
}

test.describe('Accessibilité des pages publiques', () => {
  for (const [nom, chemin] of [['accueil', '/'], ['connexion', '/connexion'], ['inscription', '/inscription']]) {
    test(nom, async ({ page }, testInfo) => {
      await page.goto(chemin);
      await auditer(page, testInfo, nom);
    });
  }
});

test.describe('Accessibilité de l\'espace patient (Marc)', () => {
  const pages = [
    ['tableau de bord', '/patient/tableau-de-bord'],
    ['anamnèse', '/patient/anamnese'],
    ['scanner', '/patient/scanner'],
    ['historique', '/patient/historique'],
    ['alertes', '/patient/alertes'],
    ['rappels', '/patient/rappels'],
    ['partage', '/patient/partage'],
  ];
  for (const [nom, chemin] of pages) {
    test(nom, async ({ page }, testInfo) => {
      await seConnecter(page, COMPTES.marc);
      await page.goto(chemin);
      await auditer(page, testInfo, `patient-${nom}`);
    });
  }
});

test.describe('Accessibilité de l\'espace médecin (Dr Mansour)', () => {
  const pages = [
    ['tableau de bord', '/medecin/tableau-de-bord'],
    ['mes patients', '/medecin/patients'],
    ['fiche patient', '/medecin/patients/4'],
    ['alertes', '/medecin/alertes'],
  ];
  for (const [nom, chemin] of pages) {
    test(nom, async ({ page }, testInfo) => {
      await seConnecter(page, COMPTES.drMansour);
      await page.goto(chemin);
      await auditer(page, testInfo, `medecin-${nom}`);
    });
  }
});