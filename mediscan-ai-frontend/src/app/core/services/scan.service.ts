import { HttpClient, HttpContext, HttpEvent } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { SANS_CHARGEMENT } from '../interceptors/contexte-http';
import { Scan } from '../models';

/**
 * Envoi des photos de médicaments et récupération des images (/api/patient/scans).
 */
@Service()
export class ScanService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/patient/scans`;

  /**
   * Envoie l'image en multipart/form-data (champ "image").
   * observe: 'events' + reportUploadProgress : on reçoit la progression de l'envoi,
   * puis la réponse finale (le scan créé). Nécessite withXhr() dans app.config.ts.
   * SANS_CHARGEMENT : la page affiche sa propre barre de progression.
   */
  envoyer(image: Blob, nomFichier = 'medicament.jpg'): Observable<HttpEvent<Scan>> {
    const formulaire = new FormData();
    formulaire.append('image', image, nomFichier);
    return this.http.post<Scan>(this.url, formulaire, {
      observe: 'events',
      reportUploadProgress: true,
      context: new HttpContext().set(SANS_CHARGEMENT, true),
    });
  }

  /** Image d'un scan (protégée par le token : on la télécharge, on ne la référence pas par URL). */
  getImage(scanId: number): Observable<Blob> {
    return this.http.get(`${this.url}/${scanId}/image`, { responseType: 'blob' });
  }
}