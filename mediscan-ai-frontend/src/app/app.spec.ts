import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('crée le composant racine', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('contient la zone d\'affichage des pages (router-outlet)', () => {
    const fixture = TestBed.createComponent(App);
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('router-outlet')).not.toBeNull();
  });
});