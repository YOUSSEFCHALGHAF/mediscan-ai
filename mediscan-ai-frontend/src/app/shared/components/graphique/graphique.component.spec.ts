import { ComponentFixture, TestBed } from '@angular/core/testing';
import type { ChartConfiguration } from 'chart.js';

import { ChargeurGraphiques } from '../../../core/graphiques/chargeur-graphiques';
import { GraphiqueComponent } from './graphique.component';

const CONFIG: ChartConfiguration = { type: 'bar', data: { labels: ['a'], datasets: [{ data: [1] }] } };

describe('GraphiqueComponent', () => {
  let fixture: ComponentFixture<GraphiqueComponent>;
  const instance = { mettreAJour: vi.fn(), detruire: vi.fn() };
  const chargeur = { creer: vi.fn(async () => instance) };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [GraphiqueComponent],
      providers: [{ provide: ChargeurGraphiques, useValue: chargeur }],
    }).compileComponents();
    fixture = TestBed.createComponent(GraphiqueComponent);
    fixture.componentRef.setInput('config', CONFIG);
    fixture.componentRef.setInput('description', '1 alerte');
    await fixture.whenStable();
  });

  it('crée le graphique sur le canvas, avec un texte alternatif', () => {
    const canvas = (fixture.nativeElement as HTMLElement).querySelector('canvas')!;
    expect(chargeur.creer).toHaveBeenCalledWith(canvas, CONFIG);
    expect(canvas.getAttribute('role')).toBe('img');
    expect(canvas.getAttribute('aria-label')).toBe('1 alerte');
  });

  it('met à jour le graphique existant quand les données changent, et le détruit à la fin', async () => {
    const nouvelle: ChartConfiguration = { ...CONFIG, data: { labels: ['b'], datasets: [{ data: [2] }] } };
    fixture.componentRef.setInput('config', nouvelle);
    await fixture.whenStable();
    expect(instance.mettreAJour).toHaveBeenCalledWith(nouvelle);
    expect(chargeur.creer).toHaveBeenCalledTimes(1);

    fixture.destroy();
    expect(instance.detruire).toHaveBeenCalled();
  });
});