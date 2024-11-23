import { HttpClient } from '@angular/common/http';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { Provider, EnvironmentProviders } from '@angular/core';
import { TranslateModule, TranslateLoader } from '@ngx-translate/core';

export function HttpLoaderFactory(http: HttpClient): TranslateHttpLoader {
	return new TranslateHttpLoader(http, 'assets/i18n/', '.json');
}

export function provideTranslateModule(): (Provider | EnvironmentProviders)[] {
	return TranslateModule.forRoot({
		loader: {
			provide: TranslateLoader,
			useFactory: HttpLoaderFactory,
			deps: [HttpClient],
		},
	}).providers!;
}
