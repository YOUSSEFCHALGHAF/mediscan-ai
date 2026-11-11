import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { SANS_CHARGEMENT, SANS_ERREUR_GLOBALE } from '../interceptors/contexte-http';
import { CategoriePartage, MedecinPublic, Partage, PartageRequest, PerimetreRequest } from '../models';

/** Consentements de partage du patient avec ses médecins (/api/patient/partages). */
@Service()
export class PartageService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/patient`;

  /** Tous mes partages, actifs et révoqués (historique). */
  lister(): Observable<Partage[]> {
    return this.http.get<Partage[]>(`${this.url}/partages`);
  }

  /** Recherche par nom, prénom ou spécialité (2 caractères minimum), pendant la frappe. */
  rechercherMedecins(terme: string): Observable<MedecinPublic[]> {
    return this.http.get<MedecinPublic[]>(`${this.url}/medecins`, {
      params: { q: terme },
      context: new HttpContext().set(SANS_CHARGEMENT, true).set(SANS_ERREUR_GLOBALE, true),
    });
  }

  /** Donne accès à un médecin. */
  creer(medecinId: number, categories: CategoriePartage[]): Observable<Partage> {
    const corps: PartageRequest = { medecinId, categories };
    return this.http.post<Partage>(`${this.url}/partages`, corps);
  }

  /**
   * Modifie le périmètre. Pour la traçabilité, le serveur révoque l'ancien consentement
   * et en crée un nouveau : le partage renvoyé a donc un NOUVEL identifiant.
   */
  modifierPerimetre(partageId: number, categories: CategoriePartage[]): Observable<Partage> {
    const corps: PerimetreRequest = { categories };
    return this.http.put<Partage>(`${this.url}/partages/${partageId}`, corps);
  }

  /** Révocation immédiate (la ligne est conservée dans l'historique). */
  revoquer(partageId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/partages/${partageId}`);
  }
}