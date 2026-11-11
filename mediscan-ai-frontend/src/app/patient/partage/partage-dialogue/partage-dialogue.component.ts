import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';

import { CategoriePartage, MedecinPublic, Partage } from '../../../core/models';
import { PartageService } from '../../../core/services/partage.service';
import { messageErreurApi } from '../../../core/utils/erreurs-api';

/** Données : partage à modifier (ou null pour un nouveau), et médecins ayant déjà accès. */
export interface DonneesPartage {
  partage: Partage | null;
  medecinsAutorises: number[];
}

/** Ce que chaque catégorie permet concrètement au médecin de voir. */
export const DESCRIPTION_CATEGORIES: { valeur: CategoriePartage; titre: string; detail: string }[] = [
  { valeur: 'ANAMNESE', titre: 'Anamnèse', detail: 'Antécédents, allergies, pathologies, traitements en cours, habitudes de vie.' },
  { valeur: 'SCANS', titre: 'Médicaments scannés', detail: 'Médicaments identifiés, numéros de lot et dates d\'expiration.' },
  { valeur: 'ALERTES', titre: 'Alertes médicamenteuses', detail: 'Allergies, interactions et contre-indications détectées par l\'IA.' },
  { valeur: 'RAPPELS', titre: 'Rappels de prise', detail: 'Vos rappels : médicament, heure et fréquence.' },
];

/** Au moins une case cochée. */
function auMoinsUne(groupe: AbstractControl): ValidationErrors | null {
  return Object.values(groupe.value as Record<string, boolean>).some(Boolean) ? null : { aucuneCategorie: true };
}

/**
 * Donner accès à un médecin (recherche + catégories), ou modifier les catégories d'un accès existant.
 * Aucune case n'est cochée par défaut : le patient choisit explicitement ce qu'il partage (RGPD).
 */
@Component({
  selector: 'app-partage-dialogue',
  imports: [
    ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatAutocompleteModule,
    MatCheckboxModule, MatButtonModule, MatIconModule,
  ],
  templateUrl: './partage-dialogue.component.html',
  styleUrl: './partage-dialogue.component.scss',
})
export class PartageDialogueComponent {
  private readonly partageService = inject(PartageService);
  private readonly dialogue = inject<MatDialogRef<PartageDialogueComponent, Partage>>(MatDialogRef);
  protected readonly donnees = inject<DonneesPartage>(MAT_DIALOG_DATA);

  protected readonly categories = DESCRIPTION_CATEGORIES;
  protected readonly modification = this.donnees.partage !== null;
  protected readonly enregistrement = signal(false);
  protected readonly erreur = signal<string | null>(null);

  protected readonly recherche = new FormControl<string | MedecinPublic>('', { nonNullable: true });
  protected readonly medecin = signal<MedecinPublic | null>(this.donnees.partage?.medecin ?? null);

  protected readonly cases = inject(NonNullableFormBuilder).group(
    {
      ANAMNESE: this.coche('ANAMNESE'),
      SCANS: this.coche('SCANS'),
      ALERTES: this.coche('ALERTES'),
      RAPPELS: this.coche('RAPPELS'),
    },
    { validators: auMoinsUne },
  );

  /** Médecins trouvés (300 ms après la frappe, 2 caractères minimum). */
  protected readonly resultats = toSignal(
    this.recherche.valueChanges.pipe(
      tap((valeur) => typeof valeur === 'string' && this.medecin.set(null)),
      map((valeur) => (typeof valeur === 'string' ? valeur.trim() : '')),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((terme) =>
        terme.length < 2
          ? of([] as MedecinPublic[])
          : this.partageService.rechercherMedecins(terme).pipe(catchError(() => of([] as MedecinPublic[]))),
      ),
    ),
    { initialValue: [] as MedecinPublic[] },
  );

  protected readonly peutValider = computed(() => !!this.medecin() && !this.enregistrement());

  protected readonly afficher = (m: MedecinPublic | string | null): string =>
    typeof m === 'string' ? m : m ? `Dr ${m.prenom} ${m.nom}` : '';

  protected dejaAutorise(m: MedecinPublic): boolean {
    return this.donnees.medecinsAutorises.includes(m.id);
  }

  protected choisir(evenement: MatAutocompleteSelectedEvent): void {
    this.medecin.set(evenement.option.value as MedecinPublic);
  }

  protected valider(): void {
    const medecin = this.medecin();
    if (!medecin || this.cases.invalid) {
      this.cases.markAllAsTouched();
      return;
    }
    const choisies = this.categories.map((c) => c.valeur).filter((c) => this.cases.controls[c].value);
    const partage = this.donnees.partage;
    this.enregistrement.set(true);
    this.erreur.set(null);
    (partage
      ? this.partageService.modifierPerimetre(partage.id, choisies)
      : this.partageService.creer(medecin.id, choisies)
    ).subscribe({
      next: (resultat) => this.dialogue.close(resultat),
      error: (err) => {
        this.enregistrement.set(false);
        this.erreur.set(messageErreurApi(err));
      },
    });
  }

  private coche(categorie: CategoriePartage): boolean {
    return this.donnees.partage?.categories.includes(categorie) ?? false;
  }
}