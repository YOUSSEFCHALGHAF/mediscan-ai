import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { ChampPastillesComponent } from './champ-pastilles.component';

/** Composant hôte : utilise le champ comme dans un vrai formulaire. */
@Component({
  imports: [ReactiveFormsModule, ChampPastillesComponent],
  template: `<app-champ-pastilles [formControl]="controle" libelle="Allergies" [suggestions]="['pénicilline', 'latex']" />`,
})
class HoteComponent {
  controle = new FormControl<string[]>(['iode'], { nonNullable: true });
}

describe('ChampPastillesComponent', () => {
  let hote: HoteComponent;
  let champ: ChampPastillesComponent;
  let html: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HoteComponent] }).compileComponents();
    const fixture = TestBed.createComponent(HoteComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    hote = fixture.componentInstance;
    html = fixture.nativeElement as HTMLElement;
    champ = fixture.debugElement.children[0].componentInstance as ChampPastillesComponent;
  });

  it('affiche la valeur initiale du formulaire sous forme de pastille', () => {
    expect(html.querySelectorAll('mat-chip-row')).toHaveLength(1);
    expect(html.querySelector('mat-chip-row')?.textContent).toContain('iode');
  });

  it('ajoute une valeur et met à jour le formulaire', () => {
    champ.ajouter('pénicilline');
    expect(hote.controle.value).toEqual(['iode', 'pénicilline']);
    expect(hote.controle.dirty).toBe(true);
  });

  it('ignore les doublons, même avec des accents ou majuscules différents', () => {
    champ.ajouter('IODE');
    champ.ajouter('Pénicilline');
    champ.ajouter('penicilline');
    expect(hote.controle.value).toEqual(['iode', 'Pénicilline']);
  });

  it('découpe une saisie contenant des virgules', () => {
    champ.ajouter('latex, arachide ,');
    expect(hote.controle.value).toEqual(['iode', 'latex', 'arachide']);
  });

  it('retire une valeur', () => {
    champ.retirer('iode');
    expect(hote.controle.value).toEqual([]);
  });
});