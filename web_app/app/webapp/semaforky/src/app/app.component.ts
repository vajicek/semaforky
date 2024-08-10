import { Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { RouterOutlet } from "@angular/router";
import { HttpClient } from "@angular/common/http";
import { HttpClientModule } from "@angular/common/http";
import { CookieService } from "ngx-cookie-service";
import { Settings, LineOrder, SemaphoreLight } from "./settings";
import { SemaforkyMachine, State, SemaforkyState } from "./states";
import { RestClientController } from "./client";
import { Scheduler } from "./scheduler";

@Component({
  selector: "app-root",
  standalone: true,
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.css"],
  imports: [CommonModule, RouterOutlet, HttpClientModule, FormsModule],
  providers: [CookieService],
})
export class AppComponent {
  SemaphoreLight: typeof SemaphoreLight = SemaphoreLight;
  LineOrder: typeof LineOrder = LineOrder;

  title = "semaforky";
  set: number = 1;
  round: number = 1;
  roundTime: Date = new Date(0);
  line: LineOrder = LineOrder.UNDEFINED;
  countdown: number = 0;
  clockTime: number = 0;
  semaphoreLight: SemaphoreLight = SemaphoreLight.RED;

  scheduler: Scheduler;
  machine: SemaforkyMachine;
  settings: Settings;
  restClientController: RestClientController;

  pausedState: State | null = null;

  beginRoundEnabled: boolean = false;
  endRoundEnabled: boolean = false;
  startSetEnabled: boolean = false;
  stopSetEnabled: boolean = false;
  cancelSetEnabled: boolean = false;
  customSetEnabled: boolean = false;
  pauseEnabled: boolean = false;
  resumeEnabled: boolean = false;

  scanEnabled: boolean = false;
  settingsEnabled: boolean = false;
  manualControlEnabled: boolean = false;

  page: number = 1;

  constructor(
    private http: HttpClient,
    protected cookieService: CookieService
  ) {
    this.scheduler = new Scheduler(this);
    this.machine = new SemaforkyMachine(this);
    this.settings = new Settings(this);
    this.restClientController = new RestClientController(http, this);

    this.settings.loadState();
    this.updateGui();
  }

  public setCookieValue(key: string, value: string) {
    this.cookieService.set(key, value);
  }

  public getCookieValue(key: string, defaultValue: string): string {
    var value = this.cookieService.get(key);
    return value ? value : defaultValue;
  }

  public updateGui() {
    var currentState = this.machine.getCurrentState();
    if (!currentState) {
      return;
    }

    let stateName = currentState.name;

    this.updateSet();

    this.beginRoundEnabled = [
      SemaforkyState.STARTED,
      SemaforkyState.ROUND_STOPPED,
    ].includes(stateName);
    this.endRoundEnabled = [
      SemaforkyState.START_WAITING,
      SemaforkyState.ROUND_STARTED,
      SemaforkyState.SET_STOPPED,
      SemaforkyState.SET_CANCELED,
      SemaforkyState.FIRE,
      SemaforkyState.WARNING,
    ].includes(stateName);
    this.startSetEnabled = [
      SemaforkyState.ROUND_STARTED,
      SemaforkyState.SET_CANCELED,
      SemaforkyState.SET_STOPPED,
    ].includes(stateName);
    this.stopSetEnabled = [
      SemaforkyState.FIRE,
      SemaforkyState.WARNING,
    ].includes(stateName);
    this.customSetEnabled = [
      SemaforkyState.SET_STOPPED,
      SemaforkyState.SET_CANCELED,
    ].includes(stateName);
    this.cancelSetEnabled = [
      SemaforkyState.READY,
      SemaforkyState.FIRE,
      SemaforkyState.WARNING,
    ].includes(stateName);
    this.pauseEnabled = [
      SemaforkyState.READY,
      SemaforkyState.FIRE,
      SemaforkyState.WARNING,
    ].includes(stateName);
    this.resumeEnabled = [SemaforkyState.SET_PAUSED].includes(stateName);

    this.scanEnabled = true;
    this.settingsEnabled = [
      SemaforkyState.ROUND_STOPPED,
      SemaforkyState.STARTED,
    ].includes(stateName);
    this.manualControlEnabled = [
      SemaforkyState.ROUND_STOPPED,
      SemaforkyState.STARTED,
    ].includes(stateName);

    if (stateName == SemaforkyState.READY) {
      this.semaphoreLight = SemaphoreLight.RED;
    } else if (stateName == SemaforkyState.FIRE) {
      this.semaphoreLight = SemaphoreLight.GREEN;
    } else if (stateName == SemaforkyState.WARNING) {
      this.semaphoreLight = SemaphoreLight.YELLOW;
    } else if (stateName != SemaforkyState.SET_PAUSED) {
      this.semaphoreLight = SemaphoreLight.NONE;
    }
  }

  private updateSet() {
    if (!this.machine.getCurrentState()) {
      return;
    }
    this.set = this.machine.getCurrentSet();
    this.round = this.machine.getCurrentRound();
    this.line = this.machine.getCurrentLineOrder();
  }

  public updateSetClocks(remainingSeconds: number) {
    this.countdown = remainingSeconds;
  }

  public updateRoundClocks(roundStart: Date) {
    this.roundTime = new Date(new Date().getTime() - roundStart.getTime());
  }

  public isVisible(semaphoreLight: SemaphoreLight) {
    return this.semaphoreLight == semaphoreLight;
  }

  public onBeginRound() {
    if (this.settings.delayedStartEnabled) {
      this.machine.moveTo(SemaforkyState.START_WAITING);
    } else {
      this.machine.moveTo(SemaforkyState.ROUND_STARTED);
    }
  }

  public onEndRound() {
    this.machine.moveTo(SemaforkyState.ROUND_STOPPED);
  }

  public onStartSet() {
    this.machine.moveTo(SemaforkyState.SET_STARTED);
  }

  public onStopSet() {
    this.machine.moveTo(SemaforkyState.SET_STOPPED);
  }

  public onCancelSet() {
    this.machine.moveTo(SemaforkyState.SET_CANCELED);
  }

  public onPauseSet() {
    this.machine.moveTo(SemaforkyState.SET_PAUSED);
  }

  public onResumeSet() {
    if (this.pausedState != null) {
      this.machine.moveTo(this.pausedState.name);
    }
  }

  public onCustomSet() {
    this.machine.moveTo(SemaforkyState.CUSTOM_SET_STARTED);
  }

  public onScan() {
    this.restClientController.scan();
  }

  public onSettings() {
    this.page = 2;
  }

  public onManualControl() {
    this.page = 3;
  }

  public onSettingsAccepted() {
    this.page = 1;
    this.settings.storeState();
  }

  public onSettingsCanceled() {
    this.page = 1;
    this.settings.loadState();
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

  public onSetClockCountdown(countdown: number) {
    this.restClientController.countdown(countdown, true);
  }

  public onBackToMain() {
    this.page = 1;
  }
}
