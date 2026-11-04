import { HttpContextToken } from '@angular/common/http';

/**
 * Options attachées à une requête HTTP pour modifier le comportement des intercepteurs.
 * Exemple :
 *   this.http.get(url, { context: new HttpContext().set(SANS_ERREUR_GLOBALE, true) })
 */

/** La requête n'affiche pas la barre de chargement globale (ex. vérification en arrière-plan). */
export const SANS_CHARGEMENT = new HttpContextToken<boolean>(() => false);

/** Les erreurs de la requête ne déclenchent pas de notification globale (le composant les gère lui-même). */
export const SANS_ERREUR_GLOBALE = new HttpContextToken<boolean>(() => false);