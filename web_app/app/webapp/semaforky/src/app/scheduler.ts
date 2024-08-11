import { Settings } from "./settings";
import { SemaforkyState } from "./states";
import { AppComponent } from "./app.component";

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
    private semaforky: AppComponent,
    private setTiming: SetTiming
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
      this.semaforky.restClientController.updateClocks(
        Math.round(remainingSeconds)
      );
      this.previousValue = remainingSeconds;
    }

    this.semaforky.updateSetClocks(remainingSeconds);

    // plan the event again
    this.semaforky.scheduler.addEvent(
      new SetClockEvent(
        new Date(now.getTime() + 100),
        this.setStart,
        this.semaforky,
        this.setTiming
      )
    );
  }

  public override timeShift(diff: number) {
    super.timeShift(diff);
    this.setStart = new Date(this.setStart.getTime() + diff);
  }

  private getRemainingSeconds(seconds: number): number {
    let remainingSeconds: number = 0;
    if (!this.semaforky.machine.getCurrentState()) {
      return -1;
    }

    let currentStateName = this.semaforky.machine.getCurrentState()?.name;

    if (currentStateName == SemaforkyState.START_WAITING) {
      let sec: number =
        (this.semaforky.settings.getDelayedStartTime().getTime() -
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
      remainingSeconds = Math.max(this.semaforky.settings.setTime - seconds, 0);
    }

    return remainingSeconds;
  }
}

class SemaphoreEvent extends Event {
  constructor(
    _time: Date,
    private nextState: SemaforkyState,
    private semaforky: AppComponent
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
    this.semaforky.machine.moveTo(this.nextState);
  }
}

class RoundClockEvent extends Event {
  constructor(
    _time: Date,
    private roundStart: Date,
    private semaforky: AppComponent
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
    this.semaforky.updateRoundClocks(this.roundStart);
    this.semaforky.scheduler.addEvent(
      new RoundClockEvent(
        new Date(now.getTime() + 200),
        this.roundStart,
        this.semaforky
      )
    );
  }
}

export class Scheduler {
  private events: PriorityQueue<Event> = new PriorityQueue<Event>(comparator);
  private paused: Date | null = null;

  constructor(
    private semaforky: AppComponent
  ) {
    let self = this;
    this.loadState();
    setInterval(() => {
      self.timerHandler();
    }, 50);
  }

  private storeState(this: Scheduler) {
    let settings = this.semaforky.settings;
    var eventList = [];
    for (var ev of this.events.toArray()) {
      eventList.push(ev.serialize());
    }
    settings.setCookieValue("events", JSON.stringify(eventList));
  }

  private jsonObjectToEvent(this: Scheduler, event: any): Event | null {
    switch (event["type"]) {
      case "RoundClockEvent": {
        return new RoundClockEvent(
          new Date(event["time"]),
          new Date(event["roundStart"]),
          this.semaforky
        );
      }
      case "SetClockEvent": {
        return new SetClockEvent(
          new Date(event["time"]),
          new Date(event["setStart"]),
          this.semaforky,
          new SetTiming(event["preparationTimeTime"], event["setTime"])
        );
      }
      case "SemaphoreEvent": {
        return new SemaphoreEvent(
          new Date(event["time"]),
          event["nextState"],
          this.semaforky
        );
      }
      default: {
        console.log("Invalid event");
        return null;
      }
    }
  }

  private loadState(this: Scheduler) {
    let settings = this.semaforky.settings;
    var jsonArray = JSON.parse(settings.getCookieValue("events", "[]"));
    for (var i = 0; i < jsonArray.length; i++) {
      var jsonObject = jsonArray[i];
      var event = this.jsonObjectToEvent(jsonObject);
      if (event) {
        this.events.enqueue(event);
      }
    }
  }

  private timerHandler(this: Scheduler) {
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

    this.addEvent(
      new SemaphoreEvent(now, SemaforkyState.READY, this.semaforky)
    );
    this.addEvent(
      new SemaphoreEvent(
        new Date(now.getTime() + settings.preparationTime * 1000 - 500),
        SemaforkyState.FIRE,
        this.semaforky
      )
    );
    this.addEvent(
      new SemaphoreEvent(
        new Date(
          now.getTime() +
          (setTime + settings.preparationTime - settings.warningTime) * 1000
        ),
        SemaforkyState.WARNING,
        this.semaforky
      )
    );
    this.addEvent(
      new SemaphoreEvent(
        new Date(
          now.getTime() + (settings.preparationTime + setTime) * 1000 + 500
        ),
        SemaforkyState.SET_STOPPED,
        this.semaforky
      )
    );

    this.addEvent(
      new SetClockEvent(
        now,
        now,
        this.semaforky,
        new SetTiming(settings.preparationTime, setTime)
      )
    );
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
    this.addEvent(
      new SemaphoreEvent(
        settings.getDelayedStartTime(),
        SemaforkyState.ROUND_STARTED,
        this.semaforky
      )
    );

    // high frequency clock event
    this.addEvent(
      new SetClockEvent(
        new Date(),
        new Date(),
        this.semaforky,
        new SetTiming(settings.preparationTime, settings.setTime)
      )
    );
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

  public pause(this: Scheduler) {
    this.paused = new Date();
  }

  public resume(this: Scheduler) {
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

  private removeAllEventsByClass(this: Scheduler, eventType: any) {
    this.events.remove((a) => a instanceof eventType);
    this.storeState();
  }

  private cancelSetEvents(this: Scheduler) {
    this.removeAllEventsByClass(SemaphoreEvent);
    this.removeAllEventsByClass(SetClockEvent);
  }
}
