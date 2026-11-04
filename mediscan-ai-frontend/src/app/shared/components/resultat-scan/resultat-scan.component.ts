import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { Medicament, Scan } from '../../../core/models';
import { etatExpiration } from '../../../core/utils/dates';
import { decouperListe } from '../../../core/utils/texte';

/**
 * Affiche le résultat de l'analyse IA d'un scan :
 * statut, médicament reconnu (fiche complète), lot, expiration, confiance OCR, texte lu.
 * Réutilisé par la page Scanner et l'historique (étape 15).
 */
@Component({
  selector: 'app-resultat-scan',
  imports: [DatePipe, DecimalPipe, MatIconModule, MatExpansionModule, MatProgressBarModule],
  templateUrl: './resultat-scan.component.html',
  styleUrl: './resultat-scan.component.scss',
})
export class ResultatScanComponent {
  readonly scan = input.required<Scan>();
  /** Fiche complète du médicament (posologie, contre-indications...), si reconnu. */
  readonly medicament = input<Medicament | null>(null);

  protected readonly etatExpiration = computed(() => etatExpiration(this.scan().dateExpiration));
  protected readonly contreIndications = computed(() => decouperListe(this.medicament()?.contreIndications));
  protected readonly interactions = computed(() => decouperListe(this.medicament()?.interactions));

  /** Titre, icône et couleur selon le statut du scan. */
  protected readonly entete = computed(() => {
    switch (this.scan().statut) {
      case 'TERMINE':
        return { icone: 'verified', titre: 'Médicament reconnu', classe: 'succes' };
      case 'A_VERIFIER':
        return { icone: 'help', titre: 'Médicament non identifié avec certitude', classe: 'attention' };
      case 'ECHEC':
        return { icone: 'error', titre: 'Aucun texte lisible sur la photo', classe: 'echec' };
      default:
        return { icone: 'hourglass_top', titre: 'Analyse en cours', classe: 'attention' };
    }
  });
}