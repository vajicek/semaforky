import {
  Injectable,
  Component,
  ViewChild,
  ElementRef
} from "@angular/core";
import { FormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";
import {
  Router,
  RouterOutlet,
  RouterLink,
  RouterLinkActive
} from '@angular/router';
import { HttpClientModule } from "@angular/common/http";
import { Subject } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';

import { Settings, LineOrder, SemaphoreLight } from "../settings";
import { SemaforkyMachine, State, SemaforkyState } from "../states";
import { RestClientController } from "../client";
import { Scheduler } from "../scheduler";

export class MessageEvent<T> {
  private eventSubject = new Subject<T>();
  event$ = this.eventSubject.asObservable();
  emit(message: T) {
    this.eventSubject.next(message);
  }
};

@Injectable({
  providedIn: 'root'
})
export class MainComponentEventBus {
  // inbound
  public updateGui: MessageEvent<void> = new MessageEvent();
  public updateSetClocks: MessageEvent<number> = new MessageEvent();
  public updateRoundClocks: MessageEvent<Date> = new MessageEvent();
  public scanEnabled: MessageEvent<boolean> = new MessageEvent();
  public pausedState: MessageEvent<State> = new MessageEvent();
  // outbound
  public countdown: MessageEvent<number> = new MessageEvent();
}

@Component({
  selector: 'app-main',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    HttpClientModule,
    FormsModule,
    TranslateModule
  ],
  templateUrl: './main.component.html',
  styleUrl: './main.component.css'
})
export class MainComponent {

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

  pausedState: State | null = null; // TODO: moved to state machine?

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

  @ViewChild('dialogCustomSet', { static: true }) dialogCustomSet!: ElementRef<HTMLDivElement>;
  @ViewChild('dialogBeginRound', { static: true }) dialogBeginRound!: ElementRef<HTMLDivElement>;

  constructor(
    private router: Router,
    public restClientController: RestClientController,
    public settings: Settings,
    public scheduler: Scheduler,
    public machine: SemaforkyMachine,
    private eventBus: MainComponentEventBus
  ) {
    this.scheduler.init();
    this.machine.init();
    this.settings.loadState();
    this.updateGui();

    eventBus.updateGui
      .event$
      .subscribe(() => {
        this.updateGui();
      });
    eventBus.updateRoundClocks
      .event$
      .subscribe(roundStart => {
        this.updateRoundClocks(roundStart);
      });
    eventBus.updateSetClocks
      .event$
      .subscribe(remainingSeconds => {
        this.updateSetClocks(remainingSeconds);
      });
    eventBus.scanEnabled
      .event$
      .subscribe(scanEnabled => {
        this.scanEnabled = scanEnabled;
      });
    eventBus.pausedState
      .event$
      .subscribe(pausedState => {
        this.pausedState = pausedState;
      });
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
    this.resumeEnabled = [
      SemaforkyState.SET_PAUSED
    ].includes(stateName);
    this.scanEnabled = !this.restClientController.isScanning();
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
    this.eventBus.countdown.emit(remainingSeconds);
  }

  public updateRoundClocks(roundStart: Date) {
    this.roundTime = new Date(new Date().getTime() - roundStart.getTime());
  }

  public isVisible(semaphoreLight: SemaphoreLight) {
    return this.semaphoreLight == semaphoreLight;
  }

  public onBeginRoundNow() {
    this.settings.continuous = false;
    this.dialogBeginRound.nativeElement.style.display = "none";
    this.machine.moveTo(SemaforkyState.ROUND_STARTED);
  }

  public onBeginRoundDelayed() {
    this.settings.continuous = false;
    this.dialogBeginRound.nativeElement.style.display = "none";
    this.machine.moveTo(SemaforkyState.START_WAITING);
  }

  public onBeginRoundContinuous() {
    this.settings.continuous = true;
    this.dialogBeginRound.nativeElement.style.display = "none";
    this.machine.moveTo(SemaforkyState.ROUND_STARTED);
  }

  public onBeginRoundCancel() {
    this.dialogBeginRound.nativeElement.style.display = "none";
  }

  public onBeginRound() {
    this.dialogBeginRound.nativeElement.style.display = "flex";
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
    this.dialogCustomSet.nativeElement.style.display = "flex";
  }

  public onCustomSetStart() {
    this.dialogCustomSet.nativeElement.style.display = "none";
    this.machine.moveTo(SemaforkyState.CUSTOM_SET_STARTED);
  }

  public onCustomSetCancel() {
    this.dialogCustomSet.nativeElement.style.display = "none";
  }

  public onScan() {
    this.restClientController.scan();
  }

  public onSettings() {
    this.router.navigate(['/settings'])
  }

  public onManualControl() {
    this.router.navigate(['/manualcontrol'])
  }
}
