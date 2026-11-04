import { inject, Service } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

type TypeMessage = 'succes' | 'erreur' | 'avertissement' | 'info';

/**
 * Notifications courtes affichées en bas de l'écran (snackbar Material).
 * Un même message n'est pas répété s'il vient d'être affiché
 * (ex. 3 requêtes en échec simultané = 1 seule notification).
 */
@Service()
export class MessagesService {
  private readonly snackBar = inject(MatSnackBar);
  private dernier: { texte: string; moment: number } | null = null;

  succes(texte: string): void {
    this.afficher(texte, 'succes', 3000);
  }

  info(texte: string): void {
    this.afficher(texte, 'info', 4000);
  }

  avertissement(texte: string, duree = 10000): void {
    this.afficher(texte, 'avertissement', duree);
  }

  erreur(texte: string): void {
    this.afficher(texte, 'erreur', 6000);
  }

  private afficher(texte: string, type: TypeMessage, duree: number): void {
    const maintenant = Date.now();
    if (this.dernier && this.dernier.texte === texte && maintenant - this.dernier.moment < 4000) {
      return;
    }
    this.dernier = { texte, moment: maintenant };
    this.snackBar.open(texte, 'OK', {
      duration: duree,
      panelClass: ['snack', `snack-${type}`],
      // Les lecteurs d'écran annoncent immédiatement les erreurs
      politeness: type === 'erreur' ? 'assertive' : 'polite',
    });
  }
}