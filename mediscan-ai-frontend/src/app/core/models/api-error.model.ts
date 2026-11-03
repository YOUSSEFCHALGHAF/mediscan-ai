/**
 * Format unique des erreurs de l'API (ApiError.java).
 * "details" contient les erreurs de validation champ par champ (400).
 */
export interface ApiError {
  status: number;
  erreur: string;
  message: string;
  chemin: string;
  timestamp: string;
  details?: Record<string, string>;
}