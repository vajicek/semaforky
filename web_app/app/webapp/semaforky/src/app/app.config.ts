import { ApplicationConfig } from '@angular/core';
import { withHashLocation, provideRouter } from '@angular/router';
import { CookieService } from 'ngx-cookie-service';
import { provideHttpClient, withInterceptorsFromDi } from "@angular/common/http";

import { routes } from './app.routes';
import { provideTranslateModule } from './translate';
import { RestClientController } from './client';
import { Settings } from './settings';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withHashLocation()),
    provideHttpClient(withInterceptorsFromDi()),
    CookieService,
    provideTranslateModule(),
    RestClientController,
    Settings
  ]
};
