import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { Component, computed, forwardRef, input, output, signal, viewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';

import { normaliser } from '../../../core/utils/texte';

/**
 * Champ de saisie en "pastilles" : on tape une valeur puis Entrée (ou virgule),
 * avec suggestions. Utilisable dans un formulaire réactif comme un champ normal :
 *   <app-champ-pastilles formControlName="allergies" libelle="Allergies" [suggestions]="..." />
 * La valeur du champ est un tableau de textes : ["pénicilline", "latex"].
 */
@Component({
  selector: 'app-champ-pastilles',
  imports: [MatFormFieldModule, MatChipsModule, MatIconModule, MatAutocompleteModule],
  templateUrl: './champ-pastilles.component.html',
  styleUrl: './champ-pastilles.component.scss',
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => ChampPastillesComponent), multi: true },
  ],
})
export class ChampPastillesComponent implements ControlValueAccessor {
  readonly libelle = input.required<string>();
  readonly placeholder = input('Tapez puis appuyez sur Entrée');
  readonly aide = input('');
  readonly suggestions = input<string[]>([]);
  /** 'alerte' : pastilles rouges (ex. allergies). */
  readonly style = input<'normal' | 'alerte'>('normal');
  /** Émis à chaque frappe : permet au parent de charger des suggestions depuis l'API. */
  readonly saisieChange = output<string>();

  private readonly declencheur = viewChild(MatAutocompleteTrigger);

  protected readonly valeurs = signal<string[]>([]);
  protected readonly saisie = signal('');
  protected readonly desactive = signal(false);
  protected readonly separateurs = [ENTER, COMMA];

  /** Suggestions correspondant à la saisie, sans celles déjà ajoutées (8 maximum). */
  protected readonly suggestionsFiltrees = computed(() => {
    const terme = normaliser(this.saisie());
    const dejaAjoutees = new Set(this.valeurs().map(normaliser));
    return this.suggestions()
      .filter((s) => !dejaAjoutees.has(normaliser(s)) && normaliser(s).includes(terme))
      .slice(0, 8);
  });

  private surChangement: (valeur: string[]) => void = () => undefined;
  protected surContact: () => void = () => undefined;

  // ---------- ControlValueAccessor : le lien avec le formulaire réactif ----------
  writeValue(valeur: string[] | null): void {
    this.valeurs.set(valeur ?? []);
  }

  registerOnChange(fn: (valeur: string[]) => void): void {
    this.surChangement = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.surContact = fn;
  }

  setDisabledState(desactive: boolean): void {
    this.desactive.set(desactive);
  }

  // ---------- Actions ----------
  /** Ajoute une ou plusieurs valeurs (une virgule collée sépare plusieurs valeurs). Ignore les doublons. */
  ajouter(texte: string): void {
    const nouvelles = texte.split(',').map((v) => v.trim()).filter(Boolean);
    let liste = this.valeurs();
    for (const valeur of nouvelles) {
      if (!liste.some((v) => normaliser(v) === normaliser(valeur))) {
        liste = [...liste, valeur];
      }
    }
    if (liste !== this.valeurs()) {
      this.valeurs.set(liste);
      this.surChangement(liste);
    }
    this.surContact();
  }

  retirer(valeur: string): void {
    const liste = this.valeurs().filter((v) => v !== valeur);
    this.valeurs.set(liste);
    this.surChangement(liste);
    this.surContact();
  }

  /** Entrée ou virgule dans la zone de saisie. */
  protected surFinSaisie(evenement: MatChipInputEvent): void {
    // Si une suggestion est surlignée, c'est elle qui sera ajoutée (surSelection)
    if (this.declencheur()?.activeOption) {
      return;
    }
    this.ajouter(evenement.value);
    evenement.chipInput.clear();
    this.saisie.set('');
  }

  /** Clic (ou Entrée) sur une suggestion. */
  protected surSelection(evenement: MatAutocompleteSelectedEvent, champ: HTMLInputElement): void {
    this.ajouter(evenement.option.value as string);
    champ.value = '';
    this.saisie.set('');
  }

  protected surFrappe(texte: string): void {
    this.saisie.set(texte);
    this.saisieChange.emit(texte);
  }
}