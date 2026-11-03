/** Réponse de GET /api/public/health (HealthController.java). */
export interface EtatSante {
  status: 'UP' | string;
  application: string;
  database: 'CONNECTEE' | 'INDISPONIBLE';
  timestamp: string;
}