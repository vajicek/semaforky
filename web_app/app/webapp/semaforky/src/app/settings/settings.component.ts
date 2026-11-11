import { Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { Settings } from "../settings";
import { RestClientController, RgbImage } from "../client";

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TranslatePipe
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

  public onImageSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    const image = new Image();
    image.onload = () => {
      if (image.naturalWidth !== 64 || image.naturalHeight !== 32) {
        alert("Image must be exactly 64x32 pixels.");
        return;
      }

      const canvas = document.createElement("canvas");
      canvas.width = 64;
      canvas.height = 32;
      const context = canvas.getContext("2d");
      if (!context) {
        alert("Unable to read image.");
        return;
      }

      context.drawImage(image, 0, 0);
      const pixels = context.getImageData(0, 0, 64, 32).data;
      const data = new Uint8Array(64 * 32 * 3);
      for (let source = 0, target = 0; target < data.length; source += 4, target += 3) {
        data[target] = pixels[source];
        data[target + 1] = pixels[source + 1];
        data[target + 2] = pixels[source + 2];
      }
      const rgbImage: RgbImage = { width: 64, height: 32, data };
      this.settings.image = rgbImage;
      if (rgbImage) {
        this.restClientController.store(rgbImage);
        this.restClientController.show();
      }
    };
    image.src = URL.createObjectURL(file);
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
