import { StatutScan } from '../models';

/** Libellé, icône et couleur d'un statut de scan, pour un affichage homogène. */
export const AFFICHAGE_STATUT: Record<StatutScan, { libelle: string; icone: string; classe: string }> = {
  TERMINE: { libelle: 'Reconnu', icone: 'verified', classe: 'succes' },
  A_VERIFIER: { libelle: 'À vérifier', icone: 'help', classe: 'attention' },
  EN_COURS: { libelle: 'En cours', icone: 'hourglass_top', classe: 'attention' },
  ECHEC: { libelle: 'Échec', icone: 'error', classe: 'echec' },
};