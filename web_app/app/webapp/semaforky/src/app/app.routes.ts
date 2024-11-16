import { Routes } from '@angular/router';

import { MainComponent } from './main/main.component';
import { ManualControlComponent } from './manualcontrol/manualcontrol.component';
import { SettingsComponent } from './settings/settings.component';

export const routes: Routes = [
	{ path: '', redirectTo: '/main', pathMatch: 'full' },
	{ path: 'main', component: MainComponent },
	{ path: 'manualcontrol', component: ManualControlComponent },
	{ path: 'settings', component: SettingsComponent }
];
