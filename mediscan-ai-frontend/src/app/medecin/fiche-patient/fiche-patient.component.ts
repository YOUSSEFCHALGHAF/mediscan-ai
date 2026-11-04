import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';

import {
  FichePatient,
  LIBELLES_CATEGORIE,
  LIBELLES_FREQUENCE,
  LIBELLES_GRAVITE,
  LIBELLES_STATUT_SCAN,
  LIBELLES_TYPE_ALERTE,
  NiveauGravite,
  Scan,
} from '../../core/models';
import { MedecinService } from '../../core/services/medecin.service';
import { heureCourte, versDateIso } from '../../core/utils/dates';
import { messageErreurApi } from '../../core/utils/erreurs-api';
import { categoriesPartagees, decouperListe } from '../../core/utils/texte';

/** Erreur d'accès à la fiche (403 = pas de consentement, 404 = patient inconnu...). */
interface ErreurFiche {
  statut: number;
  message: string;
}

const ORDRE_GRAVITE: Record<NiveauGravite, number> = { ELEVE: 0, MOYEN: 1, FAIBLE: 2 };

/**
 * Fiche synthétique d'un patient pour le médecin.
 * N'affiche QUE les catégories que le patient a choisi de partager :
 * les autres onglets sont désactivés avec un cadenas.
 */
@Component({
  selector: 'app-fiche-patient',
  imports: [
    DatePipe, LowerCasePipe, RouterLink,
    MatCardModule, MatTabsModule, MatChipsModule, MatIconModule, MatButtonModule, MatProgressBarModule, MatTooltipModule,
  ],
  templateUrl: './fiche-patient.component.html',
  styleUrl: './fiche-patient.component.scss',
})
export class FichePatientComponent implements OnInit {
  private readonly medecinService = inject(MedecinService);

  /** Paramètre ":patientId" de l'URL (fourni automatiquement grâce à withComponentInputBinding). */
  readonly patientId = input.required<string>();

  protected readonly chargement = signal(true);
  protected readonly erreur = signal<ErreurFiche | null>(null);
  protected readonly fiche = signal<FichePatient | null>(null);

  protected readonly libellesCategorie = LIBELLES_CATEGORIE;
  protected readonly libellesStatut = LIBELLES_STATUT_SCAN;
  protected readonly libellesTypeAlerte = LIBELLES_TYPE_ALERTE;
  protected readonly libellesGravite = LIBELLES_GRAVITE;
  protected readonly libellesFrequence = LIBELLES_FREQUENCE;
  protected readonly heureCourte = heureCourte;

  protected readonly categories = computed(() => categoriesPartagees(this.fiche()?.perimetreDonnees));
  protected readonly allergies = computed(() => decouperListe(this.fiche()?.anamnese?.allergies));
  protected readonly pathologies = computed(() => decouperListe(this.fiche()?.anamnese?.pathologiesChroniques));
  protected readonly traitements = computed(() => decouperListe(this.fiche()?.anamnese?.traitementsEnCours));

  /** Alertes non traitées d'abord, puis par gravité décroissante. */
  protected readonly alertesTriees = computed(() =>
    [...(this.fiche()?.alertes ?? [])].sort(
      (a, b) => Number(a.traitee) - Number(b.traitee) || ORDRE_GRAVITE[a.niveauGravite] - ORDRE_GRAVITE[b.niveauGravite],
    ),
  );
  protected readonly nbAlertesNonTraitees = computed(
    () => (this.fiche()?.alertes ?? []).filter((a) => !a.traitee).length,
  );

  private readonly aujourdhuiIso = versDateIso(new Date()) ?? '';

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set(null);
    this.medecinService.getFichePatient(Number(this.patientId())).subscribe({
      next: (fiche) => {
        this.fiche.set(fiche);
        this.chargement.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.erreur.set({ statut: err.status, message: messageErreurApi(err) });
        this.chargement.set(false);
      },
    });
  }

  /** Vrai si la date d'expiration du médicament est dépassée. */
  estPerime(scan: Scan): boolean {
    return !!scan.dateExpiration && scan.dateExpiration < this.aujourdhuiIso;
  }
}