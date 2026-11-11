/**
 * Avant les tests : vérifie que le backend répond, pour échouer tout de suite avec un message clair
 * plutôt qu'après deux minutes d'attente sur un écran de connexion.
 */
export default async function verifierBackend(): Promise<void> {
  const url = 'http://localhost:8081/api/public/health';
  try {
    const reponse = await fetch(url);
    const sante = (await reponse.json()) as { status?: string; database?: string };
    if (sante.status !== 'UP') {
      throw new Error(`état ${JSON.stringify(sante)}`);
    }
  } catch (erreur) {
    throw new Error(
      `Le backend MediScan ne répond pas sur ${url} (${String(erreur)}).\n`
      + 'Démarrez MySQL puis le backend (mvn spring-boot:run) avant de lancer les tests E2E.',
    );
  }
}