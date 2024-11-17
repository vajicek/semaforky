import { Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { Router } from '@angular/router';
import { TranslateService, TranslateModule } from '@ngx-translate/core';

import { Settings } from "../settings";
import { RestClientController } from "../client";

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TranslateModule
  ],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css'
})
export class SettingsComponent {
  constructor(
    private router: Router,
    public settings: Settings,
    private restClientController: RestClientController,
    private translate: TranslateService
  ) {
    this.settings.loadState();
  }

  public onLanguageChange(value: string) {
    this.translate.use(value);
  }

  public onBrightnessChange() {
    this.restClientController.updateBrightness();
  }

  public onSettingsAccepted() {
    this.settings.storeState();
    this.router.navigate(['/main']);
  }

  public onSettingsCanceled() {
    this.settings.loadState();
    this.router.navigate(['/main']);
  }
}
