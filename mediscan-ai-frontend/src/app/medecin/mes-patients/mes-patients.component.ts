import { BreakpointObserver } from '@angular/cdk/layout';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { map } from 'rxjs';

import { LIBELLES_CATEGORIE, PatientResume } from '../../core/models';
import { MedecinService } from '../../core/services/medecin.service';
import { messageErreurApi } from '../../core/utils/erreurs-api';
import { categoriesPartagees, normaliser } from '../../core/utils/texte';
import { PastilleAlertesComponent } from '../../shared/components/pastille-alertes/pastille-alertes.component';

/**
 * Liste des patients ayant donné leur consentement au médecin connecté.
 * Ordinateur : tableau triable. Smartphone : cartes.
 * Recherche et tri sont faits dans le navigateur (signals), sans rappeler l'API.
 */
@Component({
  selector: 'app-mes-patients',
  imports: [
    DatePipe, RouterLink,
    MatTableModule, MatSortModule, MatFormFieldModule, MatInputModule, MatIconModule, MatButtonModule,
    MatCardModule, MatChipsModule, MatProgressBarModule, PastilleAlertesComponent,
  ],
  templateUrl: './mes-patients.component.html',
  styleUrl: './mes-patients.component.scss',
})
export class MesPatientsComponent implements OnInit {
  private readonly medecinService = inject(MedecinService);

  protected readonly colonnes = ['patient', 'age', 'groupeSanguin', 'alertes', 'perimetre', 'consentement', 'actions'];
  protected readonly libellesCategorie = LIBELLES_CATEGORIE;
  protected readonly categoriesPartagees = categoriesPartagees;

  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly patients = signal<PatientResume[]>([]);
  protected readonly recherche = signal('');
  /** Tri par défaut : les patients avec le plus d'alertes en premier. */
  protected readonly tri = signal<Sort>({ active: 'alertes', direction: 'desc' });

  protected readonly estMobile = toSignal(
    inject(BreakpointObserver).observe('(max-width: 959px)').pipe(map((r) => r.matches)),
    { initialValue: false },
  );

  /** Liste filtrée (nom, prénom, sans tenir compte des accents) puis triée. */
  protected readonly patientsAffiches = computed(() => {
    const terme = normaliser(this.recherche());
    const filtres = this.patients().filter((p) => normaliser(`${p.nom} ${p.prenom}`).includes(terme)
      || normaliser(`${p.prenom} ${p.nom}`).includes(terme));

    const { active, direction } = this.tri();
    if (!direction) {
      return filtres;
    }
    const sens = direction === 'asc' ? 1 : -1;
    return [...filtres].sort((a, b) => sens * this.comparer(a, b, active));
  });

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set(null);
    this.medecinService.getPatients().subscribe({
      next: (liste) => {
        this.patients.set(liste);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreurApi(err, 'Impossible de charger la liste de vos patients.'));
        this.chargement.set(false);
      },
    });
  }

  rechercher(evenement: Event): void {
    this.recherche.set((evenement.target as HTMLInputElement).value);
  }

  private comparer(a: PatientResume, b: PatientResume, colonne: string): number {
    switch (colonne) {
      case 'age':
        return (a.age ?? -1) - (b.age ?? -1);
      case 'alertes':
        return a.alertesNonTraitees - b.alertesNonTraitees;
      case 'consentement':
        return a.dateConsentement.localeCompare(b.dateConsentement);
      default: // 'patient'
        return `${a.nom} ${a.prenom}`.localeCompare(`${b.nom} ${b.prenom}`, 'fr');
    }
  }
}