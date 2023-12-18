import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { HttpClientModule } from '@angular/common/http';
import { CookieService } from 'ngx-cookie-service';

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
  public brightness: number = 30;
  public network: string = "192.168.4.0";
  public clientsByCapability: Map<string, string[]> = new Map<string, string[]>();

  constructor(protected semaforky: AppComponent) {
  }

  private set(key: string, value: string) {
    this.semaforky.setCookieValue(key, value);
  }

  private get(key: string, defaultValue: string): string {
    return this.semaforky.getCookieValue(key, defaultValue);
  }

  public loadState() {
    this.roundSets = parseInt(this.get("roundSets", this.roundSets.toString()));
    this.setTime = parseInt(this.get("setTime", this.setTime.toString()));
    this.customSetTime = parseInt(this.get("customSetTime", this.customSetTime.toString()));
    this.preparationTime = parseInt(this.get("preparationTime", this.preparationTime.toString()));
    this.warningTime = parseInt(this.get("warningTime", this.warningTime.toString()));
    this.lines = parseInt(this.get("lines", this.lines.toString()));
    this.numberOfSets = parseInt(this.get("numberOfSets", this.numberOfSets.toString()));
    this.continuous = this.get("continuous", this.continuous.toString()) === "true";
    this.delayedStartEnabled = this.get("delayedStartEnabled", this.delayedStartEnabled.toString()) === "true";
    this.delayedStartTime = new Date(this.get("delayedStartTime", this.delayedStartTime.toString()));
    this.linesRotation = (<any>LinesRotation)[this.get("linesRotation", this.linesRotation.toString())];
    this.brightness = parseInt(this.get("brightness", this.brightness.toString()));
    this.network = this.get("network", this.network)
    this.clientsByCapability = new Map<string, string[]>(JSON.parse(this.get("clientsByCapability", "[]")));
  }

  public storeState() {
    this.set("roundSets", this.roundSets.toString());
    this.set("setTime", this.setTime.toString());
    this.set("customSetTime", this.customSetTime.toString());
    this.set("preparationTime", this.preparationTime.toString());
    this.set("warningTime", this.warningTime.toString());
    this.set("lines", this.lines.toString());
    this.set("numberOfSets", this.numberOfSets.toString());
    this.set("continuous", this.continuous.toString());
    this.set("delayedStartEnabled", this.delayedStartEnabled.toString());
    this.set("delayedStartTime", this.delayedStartTime.toString());
    this.set("linesRotation", (<any>LinesRotation)[this.linesRotation]);
    this.set("brightness", this.brightness.toString());
    this.set("network", this.network);
    this.set("clientsByCapability", JSON.stringify(Array.from(this.clientsByCapability.entries())));
  }
};

abstract class State {
  name: SemaforkyState;
  next: Array<SemaforkyState>;

  constructor(_name: SemaforkyState, _next: Array<SemaforkyState>) {
    this.name = _name;
    this.next = _next;
  }

  abstract run(previous: State): void;
}

type Request = { control: string, value: string }

class RestClientController {
  previousEncodedValue: number = 0;
  progress: number = 0;

  constructor(private http: HttpClient, private semaforky: AppComponent) {
  }

  getAllClients(): Set<string> {
    var addresses: Set<string> = new Set<string>();
    this.semaforky.settings.clientsByCapability.forEach((addressesWithCapability, capability) => {
      addressesWithCapability.forEach(addressWithCapability=>{
        if (!addresses.has(addressWithCapability)) {
          addresses.add(addressWithCapability)
        }
      });
    });
    return addresses;
  }

  updateProgess(increment: number = 0) {
    this.progress += increment;
    this.semaforky.scanEnabled = (this.progress == 0);
    this.semaforky.clients = this.getAllClients().size;
    if (this.progress == 0) {
      this.semaforky.settings.storeState();
    }
  }

  updateCapabilitiesMap(clientAddress: string, capabilities: string) {
    var capabilitiesArray: string[] = capabilities.split(",");
    capabilitiesArray.forEach(capability => {
      if (this.semaforky.settings.clientsByCapability.has(capability)) {
        this.semaforky.settings.clientsByCapability.get(capability)?.push(clientAddress);
      } else {
        this.semaforky.settings.clientsByCapability.set(capability, [clientAddress]);
      }
    });
  }

  queryCapabilities(address: string){
    var self = this;
    this.http
      .post("http://" + address + "/capabilities",
        { headers: new HttpHeaders({ timeout: `${5000}` }) })
      .subscribe({
        next(response: any) {
          self.updateCapabilitiesMap(address, response.capabilities.toString());
          self.updateProgess(-1);
        },
        error(error) {
          self.updateProgess(-1);
        },
      });
  }

  scan() {
    this.semaforky.settings.clientsByCapability.clear();
    this.updateProgess(254);
    var network = this.semaforky.settings.network;
    var lastIndex = network.lastIndexOf(".");
    var networkPrefix = network.substring(0, lastIndex + 1);
    for (var i = 1; i < 255; i++) {
      this.queryCapabilities(networkPrefix + i);
    }
  }

  getClients(capability: string): string[] {
    var retval = this.semaforky.settings.clientsByCapability.get(capability);
    if (retval == undefined) {
      return [];
    }
    return retval;
  }

  updateClocks(remainingSeconds: number) {
    let encodedValue = remainingSeconds | (this.semaforky.settings.brightness << 24);

    if (this.previousEncodedValue == encodedValue) {
      return;
    }

    this.getClients("clock").forEach(address => {
      this.http.post("http://" + address + "/control",
        { "control": 1, "value": encodedValue }
      ).subscribe();
    });

    this.previousEncodedValue = encodedValue;
  }

  updateSemaphores(state: SemaphoreLight) {
    this.getClients("semaphore").forEach(address => {
      this.http.post("http://" + address + "/control",
        { "control": 1, "value": state }
      ).subscribe();
    });
  }

  updateLines(lines: string) {
    this.getClients("lines").forEach(address => {
      this.http.post("http://" + address + "/control",
        { "control": 1, "value": lines }
      ).subscribe();
    });
  }

  playSiren(count: number) {
    this.getClients("siren").forEach(address => {
      this.http.post("http://" + address + "/control",
        { "control": 1, "value": count }
      ).subscribe();
    });
  }
}

class SemaforkyMachine {
  private states: Array<State> = [];
  private currentState: State | undefined = undefined;
  private currentSet: number = 1;
  private currentLine: number = 0;
  private customSet: boolean = false;
  private semaforky: AppComponent;

  constructor(semaforky: AppComponent) {
    this.semaforky = semaforky;
    this.initializeStates();
    this.loadState();
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
    this.storeState();
  }

  public getCurrentSet(): number {
    return this.currentSet;
  }

  public getCurrentLine(): number {
    return this.currentLine;
  }

  public getCurrentState(): State | undefined {
    return this.currentState;
  }

  public getCurrentLineOrder(): string {
    if (this.currentState?.name != SemaforkyState.STARTED && this.currentState?.name != SemaforkyState.ROUND_STOPPED) {
      if (this.semaforky.settings.lines == 1) {
        return "AB";
      } else if (this.semaforky.settings.lines == 2) {
        if (this.semaforky.settings.linesRotation == LinesRotation.SIMPLE) {
          return this.semaforky.machine.getCurrentLine() == 0 ? "AB" : "CD";
        } else {
          return this.semaforky.machine.getCurrentLine() != this.semaforky.machine.getCurrentSet() % 2 ? "AB" : "CD";
        }
      }
    }
    return "--";
  }

  protected loadState() {
    var currentStateName = this.semaforky.getCookieValue("currentState", "");
    if (currentStateName != "") {
      this.currentState = this.states.find(state => state.name.toString() == currentStateName);
    }
    this.currentSet = parseInt(this.semaforky.getCookieValue("currentSet", this.currentSet.toString()));
    this.currentLine = parseInt(this.semaforky.getCookieValue("currentLine", this.currentLine.toString()));
    this.customSet = this.semaforky.getCookieValue("customSet", this.customSet.toString()) === "true";
  }

  protected storeState() {
    if (this.currentState) {
      this.semaforky.setCookieValue("currentState", this.currentState.name.toString());
    }
    this.semaforky.setCookieValue("currentSet", this.currentSet.toString());
    this.semaforky.setCookieValue("currentLine", this.currentLine.toString());
    this.semaforky.setCookieValue("customSet", this.customSet.toString());
  }

  protected addState(state: State) {
    this.states.push(state);
    return state;
  }

  protected setCurrent(state: State) {
    this.currentState = state;
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
        self.semaforky.restClientController.updateLines(self.getCurrentLineOrder());
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
        self.semaforky.restClientController.updateLines(self.getCurrentLineOrder());
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

  abstract serialize(): Object;

  abstract run(): void;
}

class PriorityQueue<T> {
  private _items: T[];
  private _comparator: (a: T, b: T) => number;

  constructor(comparator: (a: T, b: T) => number) {
    this._items = [];
    this._comparator = comparator;
  }

  public toArray(): T[] {
    return this._items;
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

  serialize(this: SetClockEvent): Object {
    return {
      "type": "SetClockEvent",
      "time": this.time,
      "setStart": this.setStart,
      "previousValue": this.previousValue
    };
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
    if (!this.semaforky.machine.getCurrentState()) {
      return -1;
    }

    let currentStateName = this.semaforky.machine.getCurrentState()?.name;

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

  serialize(this: SemaphoreEvent): Object {
    return {
      "type": "SemaphoreEvent",
      "time": this.time,
      "nextState": this.nextState
    };
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

  serialize(this: RoundClockEvent): Object {
    return {
      "type": "RoundClockEvent",
      "time": this.time,
      "roundStart": this.roundStart
    };
  }

  run(this: RoundClockEvent): void {
    let now: Date = new Date();
    this.semaforky.updateRoundClocks(this.roundStart);
    this.semaforky.scheduler.addEvent(new RoundClockEvent(new Date(now.getTime() + 200), this.roundStart, this.semaforky));
  }
}

class Scheduler {
  private events: PriorityQueue<Event>;
  private semaforky: AppComponent;

  constructor(_semaforky: AppComponent) {
    this.semaforky = _semaforky;
    let self = this;
    this.events = new PriorityQueue<Event>(comparator);
    this.loadState();
    setInterval(() => {
      self.timerHandler();
    }, 50);
  }

  private storeState(this: Scheduler) {
    var eventList = [];
    for (var ev of this.events.toArray()) {
      eventList.push(ev.serialize());
    }
    this.semaforky.setCookieValue("events", JSON.stringify(eventList));
  }

  private jsonObjectToEvent(this: Scheduler, event: any): Event | null {
    switch (event["type"]) {
      case "RoundClockEvent": {
        return new RoundClockEvent(
          new Date(event["time"]),
          new Date(event["roundStart"]),
          this.semaforky);
      }
      case "SetClockEvent": {
        return new SetClockEvent(
          new Date(event["time"]),
          new Date(event["setStart"]),
          this.semaforky);
      }
      case "SemaphoreEvent": {
        return new SemaphoreEvent(
          new Date(event["time"]),
          event["nextState"],
          this.semaforky);
      }
      default: {
        console.log("Invalid event");
        return null;
      }
    }
  }

  private loadState(this: Scheduler) {
    var jsonArray = JSON.parse(this.semaforky.getCookieValue("events", "[]"));
    for (var i = 0; i < jsonArray.length; i++) {
      var jsonObject = jsonArray[i];
      var event = this.jsonObjectToEvent(jsonObject);
      if (event) {
        this.events.enqueue(event);
      }
    }
  }

  private timerHandler(this: Scheduler) {
    let now = new Date();
    while (this.events.length > 0 && now.getTime() > this.events.peek().time.getTime()) {
      let event = this.events.dequeue();
      this.storeState();
      if (event) {
        event.run();
      }
    }
  }

  public addEvent(this: Scheduler, event: Event) {
    this.events.enqueue(event);
    this.storeState();
  }

  public startSet(this: Scheduler) {
    this.startSetInternal(this.semaforky.settings.setTime);
  }

  private startSetInternal(this: Scheduler, setTime: number) {
    let now = new Date();
    let settings = this.semaforky.settings;

    this.cancelSetEvents();

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

  public startCustomSet(this: Scheduler) {
    this.startSetInternal(this.semaforky.settings.customSetTime);
  }

  public startRound(this: Scheduler) {
    this.addEvent(new RoundClockEvent(new Date(), new Date(), this.semaforky));
  }

  public waitForRoundStart(this: Scheduler) {
    let settings: Settings = this.semaforky.settings;

    this.cancelSetEvents();

    // start round even
    this.addEvent(new SemaphoreEvent(settings.delayedStartTime,
      SemaforkyState.ROUND_STARTED,
      this.semaforky));

    // high frequency clock event
    this.addEvent(new SetClockEvent(new Date(), new Date(), this.semaforky));
  }

  public stopSet(this: Scheduler) {
    this.cancelSetEvents();
  }

  public cancelSet(this: Scheduler) {
    this.cancelSetEvents();
  }

  public endRound(this: Scheduler) {
    this.cancelSetEvents();
    this.removeAllEventsByClass(RoundClockEvent);
  }

  private removeAllEventsByClass(this: Scheduler, eventType: any) {
    this.events.remove(a => a instanceof eventType);
    this.storeState();
  }

  private cancelSetEvents(this: Scheduler) {
    this.removeAllEventsByClass(SemaphoreEvent);
    this.removeAllEventsByClass(SetClockEvent);
  }
};

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  imports: [CommonModule, RouterOutlet, HttpClientModule, FormsModule],
  providers: [CookieService]
})
export class AppComponent {
  title = 'semaforky';
  set: number = 1;
  roundTime: Date = new Date(0);
  line: string = "";
  countdown: number = 0;

  scheduler: Scheduler;
  machine: SemaforkyMachine;
  settings: Settings;
  restClientController: RestClientController;

  color: SemaphoreLight = SemaphoreLight.RED;

  beginRoundEnabled: boolean = false;
  endRoundEnabled: boolean = false;
  startSetEnabled: boolean = false;
  stopSetEnabled: boolean = false;
  cancelSetEnabled: boolean = false;
  customSetEnabled: boolean = false;

  scanEnabled: boolean = false;
  settingsEnabled: boolean = false;
  manualControlEnabled: boolean = false;

  clients: number = 0;

  page: number = 1;

  constructor(private http: HttpClient,
    protected cookieService: CookieService) {
    this.scheduler = new Scheduler(this);
    this.machine = new SemaforkyMachine(this);
    this.settings = new Settings(this);
    this.restClientController = new RestClientController(http, this);

    this.settings.loadState();
    this.updateGui();
  }

  setCookieValue(key: string, value: string) {
    this.cookieService.set(key, value);
  }

  getCookieValue(key: string, defaultValue: string): string {
    var value = this.cookieService.get(key);
    return value ? value : defaultValue;
  }

  updateGui() {
    var currentState = this.machine.getCurrentState();
    if (!currentState) {
      return;
    }

    let stateName = currentState.name;

    this.updateSet();

    this.beginRoundEnabled = [SemaforkyState.STARTED, SemaforkyState.ROUND_STOPPED].includes(stateName);
    this.endRoundEnabled = [SemaforkyState.START_WAITING, SemaforkyState.ROUND_STARTED, SemaforkyState.SET_STOPPED, SemaforkyState.SET_CANCELED, SemaforkyState.FIRE, SemaforkyState.WARNING].includes(stateName);
    this.startSetEnabled = [SemaforkyState.ROUND_STARTED, SemaforkyState.SET_CANCELED, SemaforkyState.SET_STOPPED].includes(stateName);
    this.stopSetEnabled = [SemaforkyState.FIRE, SemaforkyState.WARNING].includes(stateName);
    this.customSetEnabled = [SemaforkyState.SET_STOPPED, SemaforkyState.SET_CANCELED].includes(stateName);
    this.cancelSetEnabled = [SemaforkyState.READY, SemaforkyState.FIRE, SemaforkyState.WARNING].includes(stateName);

    this.scanEnabled = true;
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
    if (!this.machine.getCurrentState()) {
      return;
    }
    this.set = this.machine.getCurrentSet();
    this.line = this.machine.getCurrentLineOrder();
  }

  updateSetClocks(remainingSeconds: number) {
    this.countdown = remainingSeconds;
  }

  updateRoundClocks(roundStart: Date) {
    this.roundTime = new Date((new Date()).getTime() - roundStart.getTime());
  }

  isVisible(color: number) {
    return this.color == color;
  }

  onBeginRound() {
    if (this.settings.delayedStartEnabled) {
      this.machine.moveTo(SemaforkyState.START_WAITING);
    } else {
      this.machine.moveTo(SemaforkyState.ROUND_STARTED);
    }
  }

  onEndRound() {
    this.machine.moveTo(SemaforkyState.ROUND_STOPPED);
  }

  onStartSet() {
    this.machine.moveTo(SemaforkyState.SET_STARTED);
  }

  onStopSet() {
    this.machine.moveTo(SemaforkyState.SET_STOPPED);
  }

  onCancelSet() {
    this.machine.moveTo(SemaforkyState.SET_CANCELED);
  }

  onCustomSet() {
    // TODO: finish me
    console.log("onCustomSet!");
  }

  onScan() {
    // TODO: finish me
    console.log("onDiagostic!");
    this.restClientController.scan();
  }

  onSettings() {
    this.page = 2;
  }

  onManualControl() {
    this.page = 3;
  }

  onSettingsAccepted() {
    this.page = 1;
    this.settings.storeState();
  }

  onSettingsCanceled() {
    this.page = 1;
    this.settings.loadState();
  }

  onSetSirene(beeps: number) {
  }

  onSetSemaphore(color: number) {
  }

  onSetClock(value: number) {
  }

  onSetClockCountdown(countdown: number) {
  }

  onBackToMain() {
    this.page = 1;
  }
}
