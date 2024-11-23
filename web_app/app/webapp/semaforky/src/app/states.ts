import { Injectable } from '@angular/core';

import { Settings, LineOrder, SemaphoreLight, LinesRotation } from './settings';
import { RestClientController } from './client';
import { Scheduler } from './scheduler';
import { MessageEvent, MainComponentEventBus } from './main/main.component';

export enum SemaforkyState {
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
  MANUAL_CONTROL,
  SET_PAUSED,
}

export abstract class State {
  constructor(
    public name: SemaforkyState,
    public next: Array<SemaforkyState>
  ) {
  }

  abstract run(previous: State): void;
}

@Injectable({
  providedIn: 'root'
})
export class SemaforkyMachineEventBus {
  // inbound
  public moveTo: MessageEvent<SemaforkyState> = new MessageEvent();
  // outbound
  public currentState: MessageEvent<State> = new MessageEvent();
}

@Injectable({
  providedIn: 'root'
})
export class SemaforkyMachine {
  private states: Array<State> = [];
  private currentState: State | undefined = undefined;
  private currentRound: number = 1;
  private currentSet: number = 1;
  private currentLine: number = 0;
  private countdown: number = 0;
  private customSet: boolean = false;

  constructor(
    private settings: Settings,
    private restClientController: RestClientController,
    private scheduler: Scheduler,
    private mainComponentEventBus: MainComponentEventBus,
    private eventBus: SemaforkyMachineEventBus
  ) {
    eventBus.moveTo.event$.subscribe(event => {
      this.moveTo(event);
    })
  }

  public init() {
    this.initializeStates();
    this.loadState();
    this.mainComponentEventBus.countdown.event$.subscribe(countdown => {
      this.countdown = countdown;
    });
  }

  public moveTo(stateName: SemaforkyState) {
    const state = this.states.find(({ name }) => name === stateName);
    if (state) {
      let previousState = this.currentState;
      this.setCurrentState(state);
      if (previousState) {
        state.run(previousState);
      }
    }
    this.storeState();
  }

  public getCurrentSet(): number {
    return this.currentSet;
  }

  public getCurrentRound(): number {
    return this.currentRound;
  }

  public getCurrentLine(): number {
    return this.currentLine;
  }

  public getCurrentState(): State | undefined {
    return this.currentState;
  }

  public getCurrentLineOrder(): LineOrder {
    if (
      this.currentState?.name != SemaforkyState.STARTED &&
      this.currentState?.name != SemaforkyState.ROUND_STOPPED
    ) {
      if (this.settings.lines == 1) {
        return LineOrder.AB;
      } else if (this.settings.lines == 2) {
        if (this.settings.linesRotation == LinesRotation.NO) {
          return LineOrder.AB;
        } else if (this.settings.linesRotation == LinesRotation.BYROUND) {
          return (this.getCurrentLine() +
            this.getCurrentRound()) % 2 == 1
            ? LineOrder.AB
            : LineOrder.CD;
        } else if (this.settings.linesRotation == LinesRotation.BYSET) {
          return this.getCurrentLine() !=
            this.getCurrentSet() % 2
            ? LineOrder.AB
            : LineOrder.CD;
        }
      }
    }
    return LineOrder.UNDEFINED;
  }

  protected loadState() {
    var currentStateName = this.settings.getCookieValue("currentState", "");
    if (currentStateName != "") {
      this.setCurrentState(this.states.find(
        (state) => state.name.toString() == currentStateName
      )!);
    }
    this.currentSet = parseInt(
      this.settings.getCookieValue("currentSet", this.currentSet.toString())
    );
    this.currentLine = parseInt(
      this.settings.getCookieValue("currentLine", this.currentLine.toString())
    );
    this.customSet =
      this.settings.getCookieValue("customSet", this.customSet.toString()) ===
      "true";
  }

  protected storeState() {
    if (this.currentState) {
      this.settings.setCookieValue(
        "currentState",
        this.currentState.name.toString()
      );
    }
    this.settings.setCookieValue("currentSet", this.currentSet.toString());
    this.settings.setCookieValue("currentLine", this.currentLine.toString());
    this.settings.setCookieValue("customSet", this.customSet.toString());
  }

  protected addState(state: State) {
    this.states.push(state);
    return state;
  }

  protected setCurrentState(state: State) {
    this.eventBus.currentState.emit(state);
    this.currentState = state;
  }

  protected updateGui() {
    this.mainComponentEventBus.updateGui.emit();
  }

  protected initializeStates() {
    let self = this;
    this.setCurrentState(
      this.addState(
        new (class extends State {
          run(previous: State) {
            //Nothing to do
            // TODO: LINES
          }
        })(SemaforkyState.STARTED, [
          SemaforkyState.ROUND_STARTED,
          SemaforkyState.SETTINGS,
          SemaforkyState.MANUAL_CONTROL,
          SemaforkyState.START_WAITING,
        ])
      )
    );
    this.addState(
      new (class extends State {
        run(previous: State) {
          self.restClientController.updateSemaphores(
            SemaphoreLight.NONE
          );
          self.restClientController.updateLines(
            self.getCurrentLineOrder()
          );
          self.scheduler.waitForRoundStart();
          self.updateGui();
        }
      })(SemaforkyState.START_WAITING, [
        SemaforkyState.ROUND_STARTED,
        SemaforkyState.ROUND_STOPPED,
      ])
    );
    this.addState(
      new (class extends State {
        run(previous: State) {
          self.scheduler.startRound();
          self.updateGui();
          self.currentSet = 1;
          self.currentLine = 0;
          self.moveTo(SemaforkyState.SET_STARTED);
        }
      })(SemaforkyState.ROUND_STARTED, [
        SemaforkyState.SET_STARTED,
        SemaforkyState.ROUND_STOPPED,
      ])
    );
    this.addState(
      new (class extends State {
        run(previous: State) {
          self.customSet = true;
          self.updateGui();
          self.restClientController.playSiren(2);
          self.restClientController.updateSemaphores(
            SemaphoreLight.RED
          );
          self.scheduler.startCustomSet();
        }
      })(SemaforkyState.CUSTOM_SET_STARTED, [SemaforkyState.READY])
    );
    this.addState(
      new (class extends State {
        run(previous: State) {
          self.updateGui();
          self.restClientController.playSiren(2);
          self.restClientController.updateSemaphores(
            SemaphoreLight.RED
          );
          self.scheduler.startSet();
        }
      })(SemaforkyState.SET_STARTED, [SemaforkyState.READY])
    );
    this.addState(
      new (class extends State {
        run(previous: State) {
          if (previous.name == SemaforkyState.SET_PAUSED) {
            self.scheduler.resume();
            self.updateGui();
            return;
          }
          self.updateGui();
          self.restClientController.countdown(
            self.settings.preparationTime,
            true
          );
        }
      })(SemaforkyState.READY, [
        SemaforkyState.FIRE,
        SemaforkyState.SET_CANCELED,
        SemaforkyState.SET_PAUSED,
      ])
    );
    this.addState(
      new (class extends State {
        run(previous: State) {
          if (previous.name == SemaforkyState.SET_PAUSED) {
            self.scheduler.resume();
            self.updateGui();
            return;
          }
          self.updateGui();
          self.restClientController.updateSemaphores(
            SemaphoreLight.GREEN
          );
          self.restClientController.playSiren(1);
          self.restClientController.countdown(
            self.settings.setTime,
            true
          );
        }
      })(SemaforkyState.FIRE, [
        SemaforkyState.SET_STOPPED,
        SemaforkyState.SET_CANCELED,
        SemaforkyState.WARNING,
        SemaforkyState.ROUND_STOPPED,
        SemaforkyState.SET_PAUSED,
      ])
    );
    this.addState(
      new (class extends State {
        run(previous: State) {
          if (previous.name == SemaforkyState.SET_PAUSED) {
            self.scheduler.resume();
            self.updateGui();
            return;
          }
          self.restClientController.updateSemaphores(
            SemaphoreLight.YELLOW
          );
          self.updateGui();
        }
      })(SemaforkyState.WARNING, [
        SemaforkyState.SET_CANCELED,
        SemaforkyState.SET_STOPPED,
        SemaforkyState.ROUND_STOPPED,
        SemaforkyState.SET_PAUSED,
      ])
    );
    this.addState(
      new (class extends State {
        run(previous: State) {
          self.scheduler.stopSet();
          if (!self.customSet) {
            this.updateSetAndLine();
          } else {
            self.customSet = false;
          }
          self.updateGui();
          this.updateState();
        }

        updateState() {
          if (self.currentLine == 0) {
            // remain stopped (or handle special cases) if set is over
            self.restClientController.playSiren(3);
            self.restClientController.updateSemaphores(
              SemaphoreLight.RED
            );
            self.restClientController.updateClocks(0);
            self.restClientController.countdown(
              self.countdown,
              false
            );
            // show Lines with a delay
            window.setTimeout(() => {
              self.restClientController
                .updateLines(self.getCurrentLineOrder());
            }, 2000);
            if (self.settings.continuous) {
              if (self.currentSet <= self.settings.numberOfSets) {
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
          if (self.currentLine + 1 < self.settings.lines) {
            // if number of line is higher than current line, increase line
            self.currentLine++;
          } else {
            // otherwise, increase set
            self.currentSet++;
            self.currentLine = 0;
          }
        }
      })(SemaforkyState.SET_STOPPED, [
        SemaforkyState.ROUND_STOPPED,
        SemaforkyState.SET_STARTED,
        SemaforkyState.CUSTOM_SET_STARTED,
      ])
    );

    this.addState(
      new (class extends State {
        run(previous: State) {
          self.mainComponentEventBus.pausedState.emit(previous);
          self.scheduler.pause();
          self.updateGui();
        }
      })(SemaforkyState.SET_PAUSED, [
        SemaforkyState.READY,
        SemaforkyState.FIRE,
        SemaforkyState.WARNING,
      ])
    );

    this.addState(
      new (class extends State {
        run(previous: State) {
          self.customSet = false;
          self.scheduler.cancelSet();
          self.updateGui();
          self.restClientController.updateSemaphores(
            SemaphoreLight.RED
          );
          self.restClientController.playSiren(2);
          self.restClientController.updateLines(
            self.getCurrentLineOrder()
          );
          self.restClientController.countdown(
            self.countdown,
            false
          );
        }
      })(SemaforkyState.SET_CANCELED, [
        SemaforkyState.ROUND_STOPPED,
        SemaforkyState.SET_STARTED,
        SemaforkyState.CUSTOM_SET_STARTED,
      ])
    );
    this.addState(
      new (class extends State {
        run(previous: State) {
          self.restClientController.updateSemaphores(
            SemaphoreLight.RED
          );
          self.restClientController.updateClocks(0);
          self.restClientController.countdown(0, false);
          self.mainComponentEventBus.updateSetClocks.emit(0);
          self.updateGui();
          self.currentRound++;
          // TODO: LINES
          if (previous.name != SemaforkyState.START_WAITING) {
            self.restClientController.playSiren(4);
          }
          self.scheduler.endRound();
        }
      })(SemaforkyState.ROUND_STOPPED, [
        SemaforkyState.SETTINGS,
        SemaforkyState.START_WAITING,
        SemaforkyState.ROUND_STARTED,
      ])
    );
    this.addState(
      new (class extends State {
        run(previous: State) {
          self.updateGui();
        }
      })(SemaforkyState.SETTINGS, [SemaforkyState.STARTED])
    );
    this.addState(
      new (class extends State {
        run(previous: State) {
          self.updateGui();
        }
      })(SemaforkyState.MANUAL_CONTROL, [SemaforkyState.STARTED])
    );
  }
}
