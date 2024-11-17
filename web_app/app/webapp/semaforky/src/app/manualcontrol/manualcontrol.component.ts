import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { RestClientController } from "../client";
import { LineOrder, SemaphoreLight } from "../settings";

@Component({
  selector: 'app-manualcontrol',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule
  ],
  templateUrl: './manualcontrol.component.html',
  styleUrl: './manualcontrol.component.css'
})
export class ManualControlComponent {
  SemaphoreLight: typeof SemaphoreLight = SemaphoreLight;
  LineOrder: typeof LineOrder = LineOrder;

  clockTime: number = 0;

  constructor(
    private router: Router,
    private restClientController: RestClientController
  ) {
  }

  public onSetSirene(beeps: number) {
    this.restClientController.playSiren(beeps);
  }

  public onSetSemaphore(semaphoreLight: SemaphoreLight) {
    this.restClientController.updateSemaphores(semaphoreLight);
    this.restClientController.updateClocks(this.clockTime);
  }

  public onSetClock(value: number) {
    this.clockTime = value;
    this.restClientController.updateClocks(this.clockTime);
    this.restClientController.countdown(this.clockTime, false);
  }

  public onSetLines(value: LineOrder) {
    this.restClientController.updateLines(value);
  }

  public onSetClockCountdown(countdown: number) {
    this.restClientController.countdown(countdown, true);
  }

  public onBackToMain() {
    this.router.navigate(['/main']);
  }
}
