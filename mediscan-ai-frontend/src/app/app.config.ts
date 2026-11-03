import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import localeFr from '@angular/common/locales/fr';
import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { DateAdapter, MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { MAT_ICON_DEFAULT_OPTIONS } from '@angular/material/icon';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { DateAdapterFr } from './core/utils/date-adapter-fr';

// Formats français pour les pipes date, nombre, devise (ex. "3 novembre 2026")
registerLocaleData(localeFr);

/**
 * Configuration globale de l'application (remplace l'ancien AppModule).
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    // HttpClient + intercepteur qui ajoute le token JWT à chaque appel de l'API
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
    // Langue française
    { provide: LOCALE_ID, useValue: 'fr-FR' },
    // Sélecteurs de date Material en français, saisie clavier JJ/MM/AAAA acceptée
    provideNativeDateAdapter(),
    { provide: DateAdapter, useClass: DateAdapterFr },
    { provide: MAT_DATE_LOCALE, useValue: 'fr-FR' },
    // <mat-icon> utilise la police "Material Symbols Outlined" chargée dans index.html
    { provide: MAT_ICON_DEFAULT_OPTIONS, useValue: { fontSet: 'material-symbols-outlined' } },
  ],
};