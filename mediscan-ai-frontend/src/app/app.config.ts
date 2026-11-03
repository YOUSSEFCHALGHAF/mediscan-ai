import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch } from '@angular/common/http';
import localeFr from '@angular/common/locales/fr';
import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { MAT_ICON_DEFAULT_OPTIONS } from '@angular/material/icon';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';

// Formats français pour les pipes date, nombre, devise (ex. "3 novembre 2026")
registerLocaleData(localeFr);

/**
 * Configuration globale de l'application (remplace l'ancien AppModule).
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    // HttpClient pour appeler l'API. L'intercepteur JWT sera ajouté à l'étape 11 :
    // provideHttpClient(withFetch(), withInterceptors([authInterceptor]))
    provideHttpClient(withFetch()),
    // Langue française
    { provide: LOCALE_ID, useValue: 'fr-FR' },
    // Sélecteurs de date Material en français (JJ/MM/AAAA)
    provideNativeDateAdapter(),
    { provide: MAT_DATE_LOCALE, useValue: 'fr-FR' },
    // <mat-icon> utilise la police "Material Symbols Outlined" chargée dans index.html
    { provide: MAT_ICON_DEFAULT_OPTIONS, useValue: { fontSet: 'material-symbols-outlined' } },
  ],
};