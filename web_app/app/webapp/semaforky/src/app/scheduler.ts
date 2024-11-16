import { Injectable } from '@angular/core';

import { Settings } from "./settings";
import {
  State,
  SemaforkyMachineEventBus,
  SemaforkyState
} from "./states";
import { RestClientController } from './client';
import { MainComponentEventBus } from './main/main.component';

abstract class Event {
  constructor(public time: Date) {
  }

  abstract serialize(): Object;

  abstract run(): void;

  public timeShift(diff: number) {
    this.time = new Date(this.time.getTime() + diff);
  }
}

class PriorityQueue<T> {
  private _items: T[] = [];

  constructor(private _comparator: (a: T, b: T) => number) {
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
    this._items = this._items.filter((obj) => !fnc(obj));
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
};

class SetTiming {
  constructor(
    public preparationTimeTime: number,
    public setTime: number
  ) {
  }
}

class SetClockEvent extends Event {
  previousValue: number = -1;

  constructor(
    _time: Date,
    private setStart: Date,
    private restClientController: RestClientController,
    private scheduler: Scheduler,
    private settings: Settings,
    private setTiming: SetTiming,
    private mainComponentEventBus: MainComponentEventBus
  ) {
    super(_time);
  }

  public serialize(this: SetClockEvent): Object {
    return {
      type: "SetClockEvent",
      time: this.time,
      setStart: this.setStart,
      previousValue: this.previousValue,
      preparationTimeTime: this.setTiming.preparationTimeTime,
      setTime: this.setTiming.setTime,
    };
  }

  public run(this: SetClockEvent): void {
    let now = new Date();
    let seconds = (now.getTime() - this.setStart.getTime()) / 1000;
    let remainingSeconds = this.getRemainingSeconds(seconds);

    if (remainingSeconds != this.previousValue) {
      this.restClientController.updateClocks(
        Math.round(remainingSeconds)
      );
      this.previousValue = remainingSeconds;
    }

    this.mainComponentEventBus.updateSetClocks.emit(remainingSeconds);

    // plan the event again
    this.scheduler.addEvent(
      new SetClockEvent(
        new Date(now.getTime() + 100),
        this.setStart,
        this.restClientController,
        this.scheduler,
        this.settings,
        this.setTiming,
        this.mainComponentEventBus
      ));
  }

  public override timeShift(diff: number) {
    super.timeShift(diff);
    this.setStart = new Date(this.setStart.getTime() + diff);
  }

  private getRemainingSeconds(seconds: number): number {
    let remainingSeconds: number = 0;
    if (this.scheduler.currentState == null) {
      return -1;
    }

    let currentStateName = this.scheduler.currentState?.name;

    if (currentStateName == SemaforkyState.START_WAITING) {
      let sec: number =
        (this.settings.getDelayedStartTime().getTime() -
          new Date().getTime()) /
        1000;
      remainingSeconds = Math.max(Math.min(sec, 999), 0);
    } else if (currentStateName == SemaforkyState.READY) {
      remainingSeconds = Math.max(
        this.setTiming.preparationTimeTime - seconds,
        0
      );
    } else if (
      currentStateName == SemaforkyState.FIRE ||
      currentStateName == SemaforkyState.WARNING
    ) {
      remainingSeconds = Math.max(
        this.setTiming.preparationTimeTime + this.setTiming.setTime - seconds,
        0
      );
    } else if (currentStateName == SemaforkyState.MANUAL_CONTROL) {
      remainingSeconds = Math.max(this.settings.setTime - seconds, 0);
    }

    return remainingSeconds;
  }
}

class SemaphoreEvent extends Event {
  constructor(
    _time: Date,
    private nextState: SemaforkyState,
    private semaforkyMachineEventBus: SemaforkyMachineEventBus
  ) {
    super(_time);
  }

  public serialize(this: SemaphoreEvent): Object {
    return {
      type: "SemaphoreEvent",
      time: this.time,
      nextState: this.nextState,
    };
  }

  public run(this: SemaphoreEvent): void {
    this.semaforkyMachineEventBus.moveTo.emit(this.nextState);
  }
}

class RoundClockEvent extends Event {
  constructor(
    _time: Date,
    private roundStart: Date,
    private scheduler: Scheduler,
    private mainComponentEventBus: MainComponentEventBus
  ) {
    super(_time);
  }

  public serialize(this: RoundClockEvent): Object {
    return {
      type: "RoundClockEvent",
      time: this.time,
      roundStart: this.roundStart,
    };
  }

  public run(this: RoundClockEvent): void {
    let now: Date = new Date();
    this.mainComponentEventBus.updateRoundClocks.emit(this.roundStart);
    this.scheduler.addEvent(
      new RoundClockEvent(
        new Date(now.getTime() + 200),
        this.roundStart,
        this.scheduler,
        this.mainComponentEventBus
      ));
  }
}

@Injectable({
  providedIn: 'root'
})
export class Scheduler {
  private events: PriorityQueue<Event> = new PriorityQueue<Event>(comparator);
  private paused: Date | null = null;
  currentState: State | null = null;

  constructor(
    private settings: Settings,
    private restClientController: RestClientController,
    private mainComponentEventBus: MainComponentEventBus,
    private semaforkyMachineEventBus: SemaforkyMachineEventBus
  ) {
    semaforkyMachineEventBus.currentState.event$.subscribe((state: State) => {
      this.currentState = state;
    });
  }

  public init() {
    let self = this;
    this.loadState();
    setInterval(() => {
      self.timerHandler();
    }, 50);
  }

  private storeState() {
    var eventList = [];
    for (var ev of this.events.toArray()) {
      eventList.push(ev.serialize());
    }
    this.settings.setCookieValue("events", JSON.stringify(eventList));
  }

  private jsonObjectToEvent(event: any): Event | null {
    switch (event["type"]) {
      case "RoundClockEvent": {
        return new RoundClockEvent(
          new Date(event["time"]),
          new Date(event["roundStart"]),
          this,
          this.mainComponentEventBus
        );
      }
      case "SetClockEvent": {
        return new SetClockEvent(
          new Date(event["time"]),
          new Date(event["setStart"]),
          this.restClientController,
          this,
          this.settings,
          new SetTiming(event["preparationTimeTime"], event["setTime"]),
          this.mainComponentEventBus
        );
      }
      case "SemaphoreEvent": {
        return new SemaphoreEvent(
          new Date(event["time"]),
          event["nextState"],
          this.semaforkyMachineEventBus
        );
      }
      default: {
        console.log("Invalid event");
        return null;
      }
    }
  }

  private loadState() {
    var jsonArray = JSON.parse(this.settings.getCookieValue("events", "[]"));
    for (var i = 0; i < jsonArray.length; i++) {
      var jsonObject = jsonArray[i];
      var event = this.jsonObjectToEvent(jsonObject);
      if (event) {
        this.events.enqueue(event);
      }
    }
  }

  private timerHandler() {
    if (this.paused != null) {
      return;
    }
    let now = new Date();
    while (
      this.events.length > 0 &&
      now.getTime() > this.events.peek().time.getTime()
    ) {
      let event = this.events.dequeue();
      this.storeState();
      if (event) {
        event.run();
      }
    }
  }

  public addEvent(event: Event) {
    this.events.enqueue(event);
    this.storeState();
  }

  public startSet() {
    this.startSetInternal(this.settings.setTime);
  }

  private startSetInternal(setTime: number) {
    let now = new Date();
    let settings = this.settings;

    this.cancelSetEvents();

    this.addEvent(
      new SemaphoreEvent(
        now,
        SemaforkyState.READY,
        this.semaforkyMachineEventBus
      ));
    this.addEvent(
      new SemaphoreEvent(
        new Date(now.getTime() + settings.preparationTime * 1000 - 500),
        SemaforkyState.FIRE,
        this.semaforkyMachineEventBus
      ));
    this.addEvent(
      new SemaphoreEvent(
        new Date(
          now.getTime() +
          (setTime + settings.preparationTime - settings.warningTime) * 1000
        ),
        SemaforkyState.WARNING,
        this.semaforkyMachineEventBus
      ));
    this.addEvent(
      new SemaphoreEvent(
        new Date(
          now.getTime() + (settings.preparationTime + setTime) * 1000 + 500
        ),
        SemaforkyState.SET_STOPPED,
        this.semaforkyMachineEventBus
      ));

    this.addEvent(
      new SetClockEvent(
        now,
        now,
        this.restClientController,
        this,
        this.settings,
        new SetTiming(settings.preparationTime, setTime),
        this.mainComponentEventBus
      ));
  }

  public startCustomSet() {
    this.startSetInternal(this.settings.customSetTime);
  }

  public startRound() {
    this.addEvent(new RoundClockEvent(
      new Date(),
      new Date(),
      this,
      this.mainComponentEventBus
    ));
  }

  public waitForRoundStart() {
    this.cancelSetEvents();

    // start round even
    this.addEvent(
      new SemaphoreEvent(
        this.settings.getDelayedStartTime(),
        SemaforkyState.ROUND_STARTED,
        this.semaforkyMachineEventBus
      ));

    // high frequency clock event
    this.addEvent(
      new SetClockEvent(
        new Date(),
        new Date(),
        this.restClientController,
        this,
        this.settings,
        new SetTiming(this.settings.preparationTime, this.settings.setTime),
        this.mainComponentEventBus
      ));
  }

  public stopSet() {
    this.cancelSetEvents();
  }

  public cancelSet() {
    this.cancelSetEvents();
  }

  public endRound() {
    this.cancelSetEvents();
    this.removeAllEventsByClass(RoundClockEvent);
  }

  public pause() {
    this.paused = new Date();
  }

  public resume() {
    if (this.paused == null) {
      return;
    }
    let now = new Date();
    let diff = now.getTime() - this.paused.getTime();
    for (var ev of this.events.toArray()) {
      ev.timeShift(diff);
    }
    this.paused = null;
  }

  private removeAllEventsByClass(eventType: any) {
    this.events.remove((a) => a instanceof eventType);
    this.storeState();
  }

  private cancelSetEvents() {
    this.removeAllEventsByClass(SemaphoreEvent);
    this.removeAllEventsByClass(SetClockEvent);
  }
}
