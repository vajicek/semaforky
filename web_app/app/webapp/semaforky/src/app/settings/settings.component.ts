import { Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { Router } from '@angular/router';

import { Settings } from "../settings";
import { RestClientController } from "../client";

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css'
})
export class SettingsComponent {
  constructor(
    private router: Router,
    public settings: Settings,
    private restClientController: RestClientController
  ) {
    this.settings.loadState();
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
