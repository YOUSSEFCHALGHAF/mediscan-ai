/**
 * Traduit les erreurs de getUserMedia (accès à la caméra) en messages compréhensibles.
 */
export function messageErreurCamera(erreur: unknown): string {
  const nom = erreur instanceof DOMException || erreur instanceof Error ? erreur.name : '';
  switch (nom) {
    case 'NotAllowedError':
      return 'Accès à la caméra refusé. Autorisez-le dans votre navigateur (icône 🔒 à gauche de l\'adresse), puis réessayez.';
    case 'NotFoundError':
    case 'OverconstrainedError':
      return 'Aucune caméra compatible n\'a été détectée sur cet appareil.';
    case 'NotReadableError':
      return 'La caméra est déjà utilisée par une autre application (visioconférence...). Fermez-la puis réessayez.';
    case 'SecurityError':
      return 'La caméra nécessite une connexion sécurisée (HTTPS) ou l\'adresse localhost.';
    default:
      return 'Impossible de démarrer la caméra. Vous pouvez importer une photo à la place.';
  }
}

/** Vrai si le navigateur permet d'utiliser la caméra (HTTPS ou localhost obligatoire). */
export function cameraDisponible(): boolean {
  return typeof navigator !== 'undefined' && !!navigator.mediaDevices?.getUserMedia && window.isSecureContext;
}