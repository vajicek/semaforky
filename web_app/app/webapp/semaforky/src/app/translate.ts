import { Provider } from '@angular/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { provideTranslateService } from '@ngx-translate/core';

export function provideTranslateModule(): Provider[] {
	return provideTranslateService({
		loader: provideTranslateHttpLoader({
			prefix: 'assets/i18n/',
			suffix: '.json',
			failOnError: true,
		}),
	});
}
