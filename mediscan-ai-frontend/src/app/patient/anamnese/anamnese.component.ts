import { BreakpointObserver } from '@angular/cdk/layout';
import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatStepperModule } from '@angular/material/stepper';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap } from 'rxjs';

import { AvecModificationsNonEnregistrees } from '../../core/guards/modifications.guard';
import { Anamnese, Medicament } from '../../core/models';
import { MedicamentService } from '../../core/services/medicament.service';
import { MessagesService } from '../../core/services/messages.service';
import { PatientService } from '../../core/services/patient.service';
import {
  Activite,
  Alcool,
  habitudesVersTexte,
  LIBELLES_ACTIVITE,
  LIBELLES_ALCOOL,
  LIBELLES_TABAC,
  listeVersTexte,
  Tabac,
  texteVersHabitudes,
  texteVersListe,
} from '../../core/utils/anamnese';
import { appliquerErreursChamps, messageErreurApi } from '../../core/utils/erreurs-api';
import { ChampPastillesComponent } from '../../shared/components/champ-pastilles/champ-pastilles.component';
import { messageChamp } from '../../shared/validation/messages-erreur';

/** Suggestions proposées pendant la saisie (le patient peut en saisir d'autres). */
const SUGGESTIONS_ALLERGIES = [
  'pénicilline', 'amoxicilline', 'céphalosporines', 'sulfamides', 'aspirine', 'ibuprofène', 'AINS',
  'codéine', 'iode', 'latex', 'arachide', 'fruits à coque', 'lactose', 'gluten', 'œuf', 'pollen', 'acariens',
];

const SUGGESTIONS_PATHOLOGIES = [
  'hypertension artérielle', 'diabète de type 1', 'diabète de type 2', 'asthme', 'BPCO',
  'insuffisance cardiaque', 'insuffisance rénale', 'insuffisance hépatique', 'fibrillation auriculaire',
  'ulcère gastroduodénal', 'épilepsie', 'hypothyroïdie', 'hyperthyroïdie', 'dépression',
  'polyarthrite rhumatoïde', 'grossesse en cours',
];

/**
 * Formulaire d'anamnèse en 6 étapes (stepper Material) :
 * antécédents, allergies, pathologies, traitements, habitudes de vie, récapitulatif.
 * GET /api/patient/anamnese au chargement, PUT /api/patient/anamnese à l'enregistrement.
 */
@Component({
  selector: 'app-anamnese',
  imports: [
    DatePipe, ReactiveFormsModule,
    MatStepperModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule,
    MatButtonToggleModule, MatChipsModule, MatIconModule, MatProgressBarModule,
    ChampPastillesComponent,
  ],
  templateUrl: './anamnese.component.html',
  styleUrl: './anamnese.component.scss',
  // Fermeture de l'onglet ou rafraîchissement avec des modifications : alerte du navigateur
  host: { '(window:beforeunload)': 'avantFermeture($event)' },
})
export class AnamneseComponent implements OnInit, AvecModificationsNonEnregistrees {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly patientService = inject(PatientService);
  private readonly medicamentService = inject(MedicamentService);
  private readonly messages = inject(MessagesService);

  protected readonly suggestionsAllergies = SUGGESTIONS_ALLERGIES;
  protected readonly suggestionsPathologies = SUGGESTIONS_PATHOLOGIES;
  protected readonly libellesTabac = LIBELLES_TABAC;
  protected readonly libellesAlcool = LIBELLES_ALCOOL;
  protected readonly libellesActivite = LIBELLES_ACTIVITE;
  protected readonly optionsTabac = Object.keys(LIBELLES_TABAC) as Tabac[];
  protected readonly optionsAlcool = Object.keys(LIBELLES_ALCOOL) as Alcool[];
  protected readonly optionsActivite = Object.keys(LIBELLES_ACTIVITE) as Activite[];
  protected readonly messageChamp = messageChamp;

  protected readonly estMobile = toSignal(
    inject(BreakpointObserver).observe('(max-width: 959px)').pipe(map((r) => r.matches)),
    { initialValue: false },
  );

  protected readonly form = this.fb.group({
    antecedents: ['', Validators.maxLength(5000)],
    allergies: this.fb.control<string[]>([]),
    pathologies: this.fb.control<string[]>([]),
    traitements: this.fb.control<string[]>([]),
    habitudes: this.fb.group({
      tabac: this.fb.control<Tabac | null>(null),
      alcool: this.fb.control<Alcool | null>(null),
      activite: this.fb.control<Activite | null>(null),
      remarques: ['', Validators.maxLength(1500)],
    }),
  });

  /** Valeur du formulaire sous forme de signal (pour le récapitulatif). */
  protected readonly valeur = toSignal(this.form.valueChanges.pipe(map(() => this.form.getRawValue())), {
    initialValue: this.form.getRawValue(),
  });

  protected readonly chargement = signal(true);
  protected readonly enregistrement = signal(false);
  protected readonly erreur = signal<string | null>(null);
  protected readonly derniereMaj = signal<string | null>(null);

  // ---------- Autocomplétion des traitements depuis la base médicamenteuse ----------
  private readonly rechercheTraitement = signal('');
  /**
   * debounceTime(300) : attend 300 ms sans frappe avant d'interroger l'API.
   * switchMap : annule la requête précédente si l'utilisateur continue de taper.
   * On propose la DCI (substance active), utilisée par le moteur de détection des interactions.
   */
  protected readonly suggestionsTraitements = toSignal(
    toObservable(this.rechercheTraitement).pipe(
      debounceTime(300),
      map((terme) => terme.trim()),
      distinctUntilChanged(),
      switchMap((terme) =>
        terme.length < 2
          ? of([] as Medicament[])
          : this.medicamentService.rechercher(terme).pipe(catchError(() => of([] as Medicament[]))),
      ),
      map((medicaments) => [...new Set(medicaments.map((m) => m.dci))]),
    ),
    { initialValue: [] as string[] },
  );

  ngOnInit(): void {
    this.patientService.getAnamnese().subscribe({
      next: (anamnese) => {
        this.remplir(anamnese);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreurApi(err, 'Impossible de charger votre anamnèse.'));
        this.chargement.set(false);
      },
    });
  }

  rechercherTraitement(terme: string): void {
    this.rechercheTraitement.set(terme);
  }

  enregistrer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.messages.erreur('Certains champs sont trop longs : vérifiez les étapes signalées.');
      return;
    }
    this.enregistrement.set(true);
    this.erreur.set(null);

    const v = this.form.getRawValue();
    this.patientService
      .enregistrerAnamnese({
        antecedents: v.antecedents.trim() || null,
        allergies: listeVersTexte(v.allergies),
        pathologiesChroniques: listeVersTexte(v.pathologies),
        traitementsEnCours: listeVersTexte(v.traitements),
        habitudesVie: habitudesVersTexte(v.habitudes),
      })
      .subscribe({
        next: (anamnese) => {
          this.remplir(anamnese);
          this.enregistrement.set(false);
          this.messages.succes('Anamnèse enregistrée');
        },
        error: (err) => {
          this.enregistrement.set(false);
          if (!appliquerErreursChamps(this.form, err)) {
            this.erreur.set(messageErreurApi(err));
          }
        },
      });
  }

  /** Utilisé par le guard canDeactivate. */
  aDesModificationsNonEnregistrees(): boolean {
    return this.form.dirty && !this.enregistrement();
  }

  protected avantFermeture(evenement: BeforeUnloadEvent): void {
    if (this.aDesModificationsNonEnregistrees()) {
      evenement.preventDefault();
    }
  }

  /** Remplit le formulaire avec l'anamnèse reçue du serveur (texte -> listes et choix). */
  private remplir(anamnese: Anamnese): void {
    this.derniereMaj.set(anamnese.dateMaj);
    this.form.reset({
      antecedents: anamnese.antecedents ?? '',
      allergies: texteVersListe(anamnese.allergies),
      pathologies: texteVersListe(anamnese.pathologiesChroniques),
      traitements: texteVersListe(anamnese.traitementsEnCours),
      habitudes: texteVersHabitudes(anamnese.habitudesVie),
    });
  }
}