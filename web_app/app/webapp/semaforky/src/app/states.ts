
import { AppComponent } from "./app.component";
import { LineOrder, SemaphoreLight, LinesRotation } from './settings';

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

export class SemaforkyMachine {
  private states: Array<State> = [];
  private currentState: State | undefined = undefined;
  private currentRound: number = 1;
  private currentSet: number = 1;
  private currentLine: number = 0;
  private customSet: boolean = false;

  constructor(private semaforky: AppComponent) {
  }

  public init() {
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
      if (this.semaforky.settings.lines == 1) {
        return LineOrder.AB;
      } else if (this.semaforky.settings.lines == 2) {
        if (this.semaforky.settings.linesRotation == LinesRotation.SIMPLE) {
          return (this.semaforky.machine.getCurrentLine() +
            this.semaforky.machine.getCurrentRound()) %
            2 ==
            1
            ? LineOrder.AB
            : LineOrder.CD;
        } else if (
          this.semaforky.settings.linesRotation == LinesRotation.ALTERNATING
        ) {
          return this.semaforky.machine.getCurrentLine() !=
            this.semaforky.machine.getCurrentSet() % 2
            ? LineOrder.AB
            : LineOrder.CD;
        }
      }
    }
    return LineOrder.UNDEFINED;
  }

  protected loadState() {
    var currentStateName = this.semaforky.settings.getCookieValue("currentState", "");
    if (currentStateName != "") {
      this.currentState = this.states.find(
        (state) => state.name.toString() == currentStateName
      );
    }
    this.currentSet = parseInt(
      this.semaforky.settings.getCookieValue("currentSet", this.currentSet.toString())
    );
    this.currentLine = parseInt(
      this.semaforky.settings.getCookieValue("currentLine", this.currentLine.toString())
    );
    this.customSet =
      this.semaforky.settings.getCookieValue("customSet", this.customSet.toString()) ===
      "true";
  }

  protected storeState() {
    if (this.currentState) {
      this.semaforky.settings.setCookieValue(
        "currentState",
        this.currentState.name.toString()
      );
    }
    this.semaforky.settings.setCookieValue("currentSet", this.currentSet.toString());
    this.semaforky.settings.setCookieValue("currentLine", this.currentLine.toString());
    this.semaforky.settings.setCookieValue("customSet", this.customSet.toString());
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
    this.setCurrent(
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
          self.semaforky.restClientController.updateSemaphores(
            SemaphoreLight.NONE
          );
          self.semaforky.restClientController.updateLines(
            self.getCurrentLineOrder()
          );
          self.semaforky.scheduler.waitForRoundStart();
          self.semaforky.updateGui();
        }
      })(SemaforkyState.START_WAITING, [
        SemaforkyState.ROUND_STARTED,
        SemaforkyState.ROUND_STOPPED,
      ])
    );
    this.addState(
      new (class extends State {
        run(previous: State) {
          self.semaforky.scheduler.startRound();
          self.semaforky.updateGui();
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
          self.semaforky.updateGui();
          self.semaforky.restClientController.playSiren(2);
          self.semaforky.restClientController.updateSemaphores(
            SemaphoreLight.RED
          );
          self.semaforky.scheduler.startCustomSet();
        }
      })(SemaforkyState.CUSTOM_SET_STARTED, [SemaforkyState.READY])
    );
    this.addState(
      new (class extends State {
        run(previous: State) {
          self.semaforky.updateGui();
          self.semaforky.restClientController.playSiren(2);
          self.semaforky.restClientController.updateSemaphores(
            SemaphoreLight.RED
          );
          self.semaforky.scheduler.startSet();
        }
      })(SemaforkyState.SET_STARTED, [SemaforkyState.READY])
    );
    this.addState(
      new (class extends State {
        run(previous: State) {
          if (previous.name == SemaforkyState.SET_PAUSED) {
            self.semaforky.scheduler.resume();
            self.semaforky.updateGui();
            return;
          }
          self.semaforky.updateGui();
          self.semaforky.restClientController.countdown(
            self.semaforky.settings.preparationTime,
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
            self.semaforky.scheduler.resume();
            self.semaforky.updateGui();
            return;
          }
          self.semaforky.updateGui();
          self.semaforky.restClientController.updateSemaphores(
            SemaphoreLight.GREEN
          );
          self.semaforky.restClientController.playSiren(1);
          self.semaforky.restClientController.countdown(
            self.semaforky.settings.setTime,
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
            self.semaforky.scheduler.resume();
            self.semaforky.updateGui();
            return;
          }
          self.semaforky.restClientController.updateSemaphores(
            SemaphoreLight.YELLOW
          );
          self.semaforky.updateGui();
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
            self.semaforky.restClientController.updateSemaphores(
              SemaphoreLight.RED
            );
            self.semaforky.restClientController.updateClocks(0);
            self.semaforky.restClientController.countdown(
              self.semaforky.countdown,
              false
            );
            // show Lines with a delay
            window.setTimeout(() => {
              self.semaforky
                .restClientController
                .updateLines(self.getCurrentLineOrder());
            }, 2000);
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
          if (self.currentLine + 1 < self.semaforky.settings.lines) {
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
          self.semaforky.pausedState = previous;
          self.semaforky.scheduler.pause();
          self.semaforky.updateGui();
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
          self.semaforky.scheduler.cancelSet();
          self.semaforky.updateGui();
          self.semaforky.restClientController.updateSemaphores(
            SemaphoreLight.RED
          );
          self.semaforky.restClientController.playSiren(2);
          self.semaforky.restClientController.updateLines(
            self.getCurrentLineOrder()
          );
          self.semaforky.restClientController.countdown(
            self.semaforky.countdown,
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
          self.semaforky.restClientController.updateSemaphores(
            SemaphoreLight.RED
          );
          self.semaforky.restClientController.updateClocks(0);
          self.semaforky.restClientController.countdown(0, false);
          self.semaforky.updateSetClocks(0);
          self.semaforky.updateGui();
          self.currentRound++;
          // TODO: LINES
          if (previous.name != SemaforkyState.START_WAITING) {
            self.semaforky.restClientController.playSiren(4);
          }
          self.semaforky.scheduler.endRound();
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
          self.semaforky.updateGui();
        }
      })(SemaforkyState.SETTINGS, [SemaforkyState.STARTED])
    );
    this.addState(
      new (class extends State {
        run(previous: State) {
          self.semaforky.updateGui();
        }
      })(SemaforkyState.MANUAL_CONTROL, [SemaforkyState.STARTED])
    );
  }
}
