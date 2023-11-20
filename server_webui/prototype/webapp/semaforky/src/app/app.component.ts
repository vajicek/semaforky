import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { HttpClientModule } from '@angular/common/http';
import { Injectable } from '@angular/core'


enum SemaforkyState {
  STARTED,
  START_WAITING,
  ROUND_STARTED,
  CUSTOM_SET_STARTED,
  SET_STARTED,
  READY,
  FIRE,
  WARNING,
  SET_STOPPED,
  SET_CANCELED,
  ROUND_STOPPED,
  SETTINGS,
  MANUAL_CONTROL
}

enum LinesRotation {
  SIMPLE,
  ALTERNATING
}

enum SemaphoreLight {
  NONE,
  RED,
  GREEN,
  YELLOW
}

class Settings {
  public language: number = 0;
  public roundSets: number = 10;
  public setTime: number = 120;
  public customSetTime: number = 120;
  public preparationTime: number = 10;
  public warningTime: number = 30;
  public lines: number = 1;
  public continuous: boolean = false;
  public numberOfSets: number = 10;
  public linesRotation: LinesRotation = LinesRotation.SIMPLE;
  public delayedStartEnabled: boolean = false;
  public delayedStartTime: Date = new Date(12, 0, 0);
};

abstract class State {
  name: SemaforkyState;
  next: Array<SemaforkyState>;;

  constructor(_name: SemaforkyState, _next: Array<SemaforkyState>) {
    this.name = _name;
    this.next = _next;
  }

  abstract run(previous: State): void;
}

type Request = { control: string, value: string }

class RestClientController {
  previousEncodedValue: number = 0;

  constructor(private http: HttpClient) {
  }

  updateClocks(this: RestClientController, remainingSeconds: number) {
    let encodedValue = remainingSeconds | (30 << 24);

    if (this.previousEncodedValue == encodedValue) {
      return;
    }

    this.http.post("http://192.168.4.1/control",
    //this.http.post("http://192.168.1.241/control",
    //this.http.post("http://192.168.1.213/control",
      { "control": 1, "value": encodedValue}
    ).subscribe();

    this.previousEncodedValue = encodedValue;
  }

  updateSemaphores(this: RestClientController, state: SemaphoreLight) {
    // TODO: finish me
    console.log("updateSemaphores");
  }

  playSiren(this: RestClientController, count: number) {
    // TODO: finish me
    console.log("playSirens");
  }
}

class SemaforkyMachine {
  states: Array<State> = [];
  currentState: State|null = null;
  currentSet: number = 1;
  currentLine: number = 0;
  customSet: boolean = false;
  semaforky: AppComponent;

  constructor(semaforky: AppComponent) {
    this.semaforky = semaforky;
    this.initializeStates();
  }

  addState(state: State) {
    this.states.push(state);
    return state;
  }

  setCurrent(state: State) {
    this.currentState = state;
  }

  public moveTo(stateName: SemaforkyState) {
    const state = this.states.find(({ name }) => name === stateName);
    if (state) {
      let previousState = this.currentState;
      this.setCurrent(state);
      if (previousState) {
        state.run(previousState);
      }
    }
  }

  protected initializeStates() {
    let self = this;
    this.setCurrent(this.addState(new class extends State {
      run(previous: State) {
        //Nothing to do
      }
    }(SemaforkyState.STARTED, [SemaforkyState.ROUND_STARTED, SemaforkyState.SETTINGS, SemaforkyState.MANUAL_CONTROL, SemaforkyState.START_WAITING])));
    this.addState(new class extends State {
      run(previous: State) {
        self.semaforky.restClientController.updateSemaphores(SemaphoreLight.NONE);
        self.semaforky.scheduler.waitForRoundStart();
        self.semaforky.updateGui();
      }
    }(SemaforkyState.START_WAITING, [SemaforkyState.ROUND_STARTED, SemaforkyState.ROUND_STOPPED]));
    this.addState(new class extends State {
      run(previous: State) {
        self.semaforky.scheduler.startRound();
        self.semaforky.updateGui();
        self.currentSet = 1;
        self.currentLine = 0;
        self.moveTo(SemaforkyState.SET_STARTED);
      }
    }(SemaforkyState.ROUND_STARTED, [SemaforkyState.SET_STARTED, SemaforkyState.ROUND_STOPPED]));
    this.addState(new class extends State {
      run(previous: State) {
        self.customSet = true;
        self.semaforky.updateGui();
        self.semaforky.restClientController.playSiren(2);
        self.semaforky.scheduler.startCustomSet();
      }
    }(SemaforkyState.CUSTOM_SET_STARTED, [SemaforkyState.READY]));
    this.addState(new class extends State {
      run(previous: State) {
        self.semaforky.updateGui();
        self.semaforky.restClientController.playSiren(2);
        self.semaforky.scheduler.startSet();
      }
    }(SemaforkyState.SET_STARTED, [SemaforkyState.READY]));
    this.addState(new class extends State {
      run(previous: State) {
        self.semaforky.updateGui();
      }
    }(SemaforkyState.READY, [SemaforkyState.FIRE, SemaforkyState.SET_CANCELED]));
    this.addState(new class extends State {
      run(previous: State) {
        self.semaforky.updateGui();
        self.semaforky.restClientController.playSiren(1);
      }
    }(SemaforkyState.FIRE, [SemaforkyState.SET_STOPPED, SemaforkyState.SET_CANCELED, SemaforkyState.WARNING, SemaforkyState.ROUND_STOPPED]));
    this.addState(new class extends State {
      run(previous: State) {
        self.semaforky.updateGui();
      }
    }(SemaforkyState.WARNING, [SemaforkyState.SET_CANCELED, SemaforkyState.SET_STOPPED, SemaforkyState.ROUND_STOPPED]));
    this.addState(new class extends State {
      run(previous: State) {
        self.semaforky.scheduler.stopSet();
        if (!self.customSet) {
          this.updateSetAndLine();
        } else {
          self.customSet = false;
        }
        self.semaforky.updateGui();
        this.updateState();
      }

      updateState() {
        if (self.currentLine == 0) {
          // remain stopped (or handle special cases) if set is over
          self.semaforky.restClientController.playSiren(3);
          self.semaforky.restClientController.updateClocks(0);
          self.semaforky.restClientController.updateSemaphores(SemaphoreLight.RED);
          if (self.semaforky.settings.continuous) {
            if (self.currentSet <= self.semaforky.settings.numberOfSets) {
              self.moveTo(SemaforkyState.SET_STARTED);
            } else {
              self.moveTo(SemaforkyState.ROUND_STOPPED);
            }
          }
        } else {
          // otherwise continue
          self.moveTo(SemaforkyState.SET_STARTED);
        }
      }

      updateSetAndLine() {
        if ((self.currentLine + 1) < self.semaforky.settings.lines) {
          // if number of line is higher than current line, increase line
          self.currentLine++;
        } else {
          // otherwise, increase set
          self.currentSet++;
          self.currentLine = 0;
        }
      }
    }(SemaforkyState.SET_STOPPED, [SemaforkyState.ROUND_STOPPED, SemaforkyState.SET_STARTED, SemaforkyState.CUSTOM_SET_STARTED]));
    this.addState(new class extends State {
      run(previous: State) {
        self.customSet = false;
        self.semaforky.scheduler.cancelSet();
        self.semaforky.updateGui();
        self.semaforky.restClientController.playSiren(2);
      }
    }(SemaforkyState.SET_CANCELED, [SemaforkyState.ROUND_STOPPED, SemaforkyState.SET_STARTED, SemaforkyState.CUSTOM_SET_STARTED]));
    this.addState(new class extends State {
      run(previous: State) {
        self.semaforky.restClientController.updateClocks(0);
        self.semaforky.restClientController.updateSemaphores(SemaphoreLight.RED);
        self.semaforky.updateSetClocks(0);
        self.semaforky.updateGui();
        if (previous.name != SemaforkyState.START_WAITING) {
          self.semaforky.restClientController.playSiren(4);
        }
        self.semaforky.scheduler.endRound();
      }
    }(SemaforkyState.ROUND_STOPPED, [SemaforkyState.SETTINGS, SemaforkyState.START_WAITING, SemaforkyState.ROUND_STARTED]));
    this.addState(new class extends State {
      run(previous: State) {
        self.semaforky.updateGui();
      }
    }(SemaforkyState.SETTINGS, [SemaforkyState.STARTED]));
    this.addState(new class extends State {
      run(previous: State) {
        self.semaforky.updateGui();
      }
    }(SemaforkyState.MANUAL_CONTROL, [SemaforkyState.STARTED]));
  }
}

abstract class Event {
  time: Date;

  constructor(_time: Date) {
    this.time = _time;
  }

  abstract run(): void;
}

class PriorityQueue<T> {
  private _items: T[];
  private _comparator: (a: T, b: T) => number;

  constructor(comparator: (a: T, b: T) => number) {
    this._items = [];
    this._comparator = comparator;
  }

  public enqueue(item: T): void {
    this._items.push(item);
    this._items.sort(this._comparator);
  }

  public dequeue(): T | undefined {
    return this._items.shift();
  }

  public peek(): T {
    return this._items[0];
  }

  public get length(): number {
    return this._items.length;
  }

  public remove(fnc: (obj: T) => boolean) {
    this._items = this._items.filter(obj => fnc(obj));
  }
}

const comparator = (a: Event, b: Event) => {
  if (a.time < b.time) {
    return -1;
  }

  if (a.time > b.time) {
    return 1;
  }

  return 0;
}

class SetClockEvent extends Event {
  setStart: Date;
  semaforky: AppComponent;
  previousValue: number;

  constructor(_time: Date, _start: Date, _semaforky: AppComponent) {
    super(_time)
    this.setStart = _start;
    this.semaforky = _semaforky;
    this.previousValue = -1;
  }

  run(this: SetClockEvent): void {
    let now = new Date();
    let seconds = (now.getTime() - this.setStart.getTime()) / 1000;
    let remainingSeconds = this.getRemainingSeconds(seconds);

    if (remainingSeconds != this.previousValue) {
      this.semaforky.restClientController.updateClocks(Math.round(remainingSeconds));
      this.previousValue = remainingSeconds;
    }
    this.semaforky.updateSetClocks(remainingSeconds);

    // plan the event again
    this.semaforky.scheduler.addEvent(new SetClockEvent(
      new Date(now.getTime() + 100),
      this.setStart,
      this.semaforky));
  }

  getRemainingSeconds(seconds: number): number {
    let remainingSeconds: number = 0;
    if (!this.semaforky.machine.currentState) {
      return -1;
    }

    let currentStateName = this.semaforky.machine.currentState.name;

    if (currentStateName == SemaforkyState.START_WAITING) {
      let sec: number = (this.semaforky.settings.delayedStartTime.getTime() - new Date().getTime()) / 1000;
      remainingSeconds = Math.max(Math.min(sec, 999), 0);
    } else if (currentStateName == SemaforkyState.READY) {
      remainingSeconds = Math.max(this.semaforky.settings.preparationTime - seconds, 0);
    } else if (currentStateName == SemaforkyState.FIRE || currentStateName == SemaforkyState.WARNING) {
      remainingSeconds = Math.max(this.semaforky.settings.preparationTime + this.semaforky.settings.setTime - seconds, 0);
    } else if (currentStateName == SemaforkyState.MANUAL_CONTROL) {
      remainingSeconds = Math.max(this.semaforky.settings.setTime - seconds, 0);
    }

    return remainingSeconds;
  }
}

class SemaphoreEvent extends Event {
  nextState: SemaforkyState;
  semaforky: AppComponent;

  constructor(_time: Date, _nextState: SemaforkyState, _semaforky: AppComponent) {
    super(_time)
    this.nextState = _nextState;
    this.semaforky = _semaforky;
  }

  run(this: SemaphoreEvent): void {
    this.semaforky.machine.moveTo(this.nextState);
  }
}

class RoundClockEvent extends Event {
  roundStart: Date;
  semaforky: AppComponent;

  constructor(_time: Date, _roundStart: Date, _semaforky: AppComponent) {
    super(_time)
    this.roundStart = _roundStart;
    this.semaforky = _semaforky;
  }

  run(this: RoundClockEvent): void {
    let now: Date = new Date();
    this.semaforky.updateRoundClocks(this.roundStart);
    this.semaforky.scheduler.addEvent(new RoundClockEvent(new Date(now.getTime() + 200), this.roundStart, this.semaforky));
  }
}

class Scheduler {
  private events: PriorityQueue<Event>;
  semaforky: AppComponent;

  constructor(_semaforky: AppComponent) {
    this.semaforky = _semaforky;
    let self = this;
    this.events = new PriorityQueue<Event>(comparator);
    setInterval(() => {
      self.timerHandler();
    }, 50);
  }

  timerHandler(this: Scheduler) {
    let now = new Date();
    while (this.events.length > 0 && now.getTime() > this.events.peek().time.getTime()) {
      let event = this.events.dequeue();
      if (event) {
        event.run();
      }
    }
  }

  addEvent(this: Scheduler, event: Event) {
    this.events.enqueue(event);
  }

  startSet(this: Scheduler) {
    this.startSetInternal(this.semaforky.settings.setTime);
  }

  startSetInternal(this: Scheduler, setTime: number) {
    let now = new Date();
    let settings = this.semaforky.settings;
    this.addEvent(new SemaphoreEvent(now,
      SemaforkyState.READY,
      this.semaforky));
    this.addEvent(new SemaphoreEvent(new Date(now.getTime() + settings.preparationTime * 1000),
      SemaforkyState.FIRE,
      this.semaforky));
    this.addEvent(new SemaphoreEvent(new Date(now.getTime() + (settings.setTime + settings.preparationTime - settings.warningTime) * 1000),
      SemaforkyState.WARNING,
      this.semaforky));
    this.addEvent(new SemaphoreEvent(new Date(now.getTime() + (settings.preparationTime + settings.setTime) * 1000 + 500),
      SemaforkyState.SET_STOPPED,
      this.semaforky));

    this.addEvent(new SetClockEvent(now, now, this.semaforky));
  }

  startCustomSet(this: Scheduler) {
    this.startSetInternal(this.semaforky.settings.customSetTime);
  }

  startRound(this: Scheduler) {
    this.addEvent(new RoundClockEvent(new Date(), new Date(), this.semaforky));
  }

  waitForRoundStart(this: Scheduler) {
    let settings: Settings = this.semaforky.settings;

    // start round even
    this.addEvent(new SemaphoreEvent(settings.delayedStartTime,
      SemaforkyState.ROUND_STARTED,
      this.semaforky));

    // high frequency clock event
    this.addEvent(new SetClockEvent(new Date(), new Date(), this.semaforky));
  }

  stopSet(this: Scheduler) {
    this.cancelSetEvents();
  }

  cancelSet(this: Scheduler) {
    this.cancelSetEvents();
  }

  endRound(this: Scheduler) {
    this.cancelSetEvents();
    this.removeAllEventsByClass(RoundClockEvent);
  }

  removeAllEventsByClass(this: Scheduler, eventType: any) {
    this.events.remove(a => a instanceof eventType);
  }

  cancelSetEvents(this: Scheduler) {
    this.removeAllEventsByClass(SemaphoreEvent);
    this.removeAllEventsByClass(SetClockEvent);
  }
};

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, HttpClientModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'semaforky';
  set = 1;
  roundTime: Date = new Date(0);
  line: string = "";
  countdown = 0;

  scheduler: Scheduler;
  machine: SemaforkyMachine;
  settings: Settings;
  restClientController: RestClientController;

  color: SemaphoreLight = SemaphoreLight.RED;
  redHidden = true;

  beginRoundEnabled: boolean = false;
  endRoundEnabled: boolean = false;
  startSetEnabled: boolean = false;
  stopSetEnabled: boolean = false;
  cancelSetEnabled: boolean = false;
  customSetEnabled: boolean = false;

  diagnosticEnabled: boolean = false;
  settingsEnabled: boolean = false;
  manualControlEnabled: boolean = false;

  constructor(private http: HttpClient) {
    this.scheduler = new Scheduler(this);
    this.machine = new SemaforkyMachine(this);
    this.settings = new Settings();
    this.restClientController = new RestClientController(http);

    this.updateGui();
  }

  updateGui(this: AppComponent) {
    // TODO: finish this
    console.log("updateGui");

    if (!this.machine.currentState) {
      return;
    }

    let stateName = this.machine.currentState.name;

    this.updateSet();

    this.beginRoundEnabled = [SemaforkyState.STARTED, SemaforkyState.ROUND_STOPPED].includes(stateName);
    this.endRoundEnabled = [SemaforkyState.START_WAITING, SemaforkyState.ROUND_STARTED, SemaforkyState.SET_STOPPED, SemaforkyState.SET_CANCELED, SemaforkyState.FIRE, SemaforkyState.WARNING].includes(stateName);
    this.startSetEnabled = [SemaforkyState.ROUND_STARTED, SemaforkyState.SET_CANCELED, SemaforkyState.SET_STOPPED].includes(stateName);
    this.stopSetEnabled = [SemaforkyState.FIRE, SemaforkyState.WARNING].includes(stateName);
    this.customSetEnabled = [SemaforkyState.SET_STOPPED, SemaforkyState.SET_CANCELED].includes(stateName);
    this.cancelSetEnabled = [SemaforkyState.READY, SemaforkyState.FIRE, SemaforkyState.WARNING].includes(stateName);

    this.diagnosticEnabled = true;
    this.settingsEnabled = [SemaforkyState.ROUND_STOPPED, SemaforkyState.STARTED].includes(stateName);
    this.manualControlEnabled = [SemaforkyState.ROUND_STOPPED, SemaforkyState.STARTED].includes(stateName);

    if (stateName == SemaforkyState.READY) {
      this.color = SemaphoreLight.RED;
    } else if (stateName == SemaforkyState.FIRE) {
      this.color = SemaphoreLight.GREEN;
    } else if (stateName == SemaforkyState.WARNING) {
      this.color = SemaphoreLight.YELLOW;
    } else {
      this.color = SemaphoreLight.NONE;
    }
  }

  updateSet() {
    if (!this.machine.currentState) {
      return;
    }

    let stateName = this.machine.currentState.name;

    this.set = this.machine.currentSet;

    if (stateName == SemaforkyState.STARTED || stateName == SemaforkyState.ROUND_STOPPED) {
       this.line = "--";
    } else if (this.settings.lines == 1) {
      this.line = "AB";
    } else if (this.settings.lines == 2) {
      if (this.settings.linesRotation == LinesRotation.SIMPLE) {
        this.line = this.machine.currentLine == 0 ? "AB" : "CD";
      } else {
        this.line = this.machine.currentLine != this.machine.currentSet % 2 ? "AB" : "CD";
      }
    }
  }

  updateSetClocks(this: AppComponent, remainingSeconds: number) {
    this.countdown = remainingSeconds;
  }

  updateRoundClocks(this: AppComponent, roundStart: Date) {
    this.roundTime = new Date((new Date()).getTime()  - roundStart.getTime());
  }

  isVisible(this: AppComponent, color: number) {
    return this.color == color;
  }

  onBeginRound(this: AppComponent) {
    if (this.settings.delayedStartEnabled) {
      this.machine.moveTo(SemaforkyState.START_WAITING);
    } else {
      this.machine.moveTo(SemaforkyState.ROUND_STARTED);
    }
  }

  onEndRound(this: AppComponent) {
    this.machine.moveTo(SemaforkyState.ROUND_STOPPED);
  }

  onStartSet(this: AppComponent) {
    this.machine.moveTo(SemaforkyState.SET_STARTED);
  }

  onStopSet(this: AppComponent) {
    this.machine.moveTo(SemaforkyState.SET_STOPPED);
  }

  onCancelSet(this: AppComponent) {
    this.machine.moveTo(SemaforkyState.SET_CANCELED);
  }

  onCustomSet(this: AppComponent) {
    // TODO: finish me
    console.log("onCustomSet!");
  }

  onDiagostic(this: AppComponent) {
    // TODO: finish me
    console.log("onDiagostic!");
  }

  onSettings(this: AppComponent) {
    // TODO: finish me
    console.log("onSettings!");
  }

  onManualControl() {
    // TODO: finish me
    console.log("onManualControl!");
  }
}
