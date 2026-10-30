import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Subscription } from "rxjs";

import { Settings, LineOrder, SemaphoreLight } from "./settings";
import { MainComponentEventBus } from './main/main.component';

enum ControlValue {
  Clock = 1,
  Semaphore = 2,
  Lines = 3,
  Siren = 4,
  Countdown = 5,
}

const CONTROL_CAPABILITY: Record<ControlValue, string> = {
  [ControlValue.Clock]: "clock",
  [ControlValue.Semaphore]: "semaphore",
  [ControlValue.Lines]: "lines",
  [ControlValue.Siren]: "siren",
  [ControlValue.Countdown]: "countdown",
};

@Injectable({
  providedIn: 'root'
})
export class RestClientController {
  remainingSeconds: number = 0;
  previousEncodedValue: number = 0;
  audio: any = null;
  semaphoreLight: SemaphoreLight = SemaphoreLight.NONE;
  scanProcess: Map<string, Subscription> = new Map();

  constructor(
    private http: HttpClient,
    private settings: Settings,
    private eventBus: MainComponentEventBus
  ) {
    this.audio = this.initAudio();
  }

  public getAllClients(): Set<string> {
    var addresses: Set<string> = new Set<string>();
    this.settings.clientsByCapability.forEach(
      (addressesWithCapability, capability) => {
        addressesWithCapability.forEach((addressWithCapability) => {
          if (!addresses.has(addressWithCapability)) {
            addresses.add(addressWithCapability);
          }
        });
      }
    );
    return addresses;
  }

  protected updateProgress() {
    var isScanning = this.isScanning();
    this.eventBus.scanEnabled.emit(!isScanning);
    if (!isScanning) {
      this.settings.storeState();
    }
  }

  protected updateCapabilitiesMap(clientAddress: string, capabilities: string) {
    var capabilitiesArray: string[] = capabilities.split(",");
    capabilitiesArray.forEach((capability) => {
      if (this.settings.clientsByCapability.has(capability)) {
        this.settings.clientsByCapability
          .get(capability)
          ?.push(clientAddress);
      } else {
        this.settings.clientsByCapability.set(capability, [
          clientAddress,
        ]);
      }
    });
  }

  protected queryCapabilities(address: string) {
    var self = this;
    var subscription = this.http
      .post("http://" + address + "/capabilities", {
        headers: new HttpHeaders({ timeout: `${5000}` }),
      })
      .subscribe({
        next(response: any) {
          self.updateCapabilitiesMap(address, response.capabilities.toString());
          self.scanProcess.delete(address);
          self.updateProgress();
        },
        error(error) {
          self.scanProcess.delete(address);
          self.updateProgress();
        }
      });
    this.scanProcess.set(address, subscription);
  }

  public scan() {
    this.settings.clientsByCapability.clear();
    var network = this.settings.network;
    var lastIndex = network.lastIndexOf(".");
    var networkPrefix = network.substring(0, lastIndex + 1);
    for (var i = 1; i < 255; i++) {
      var address = networkPrefix + i;
      if (this.scanProcess.has(address)) {
        this.scanProcess.get(address)?.unsubscribe();
      }
      this.queryCapabilities(address);
    }
    this.updateProgress();
  }

  public isScanning(): boolean {
    return this.scanProcess.size > 0;
  }

  public getClients(capability: string): string[] {
    var clients = this.settings.clientsByCapability.get(capability);
    if (clients == undefined) {
      return [];
    }
    return clients;
  }

  protected getEncodedValue() {
    return (
      this.remainingSeconds |
      (this.settings.brightness << 24) |
      (Number(this.semaphoreLight) << 16)
    );
  }

  public updateBrightness() {
    this.updateClocks(this.remainingSeconds);
  }

  public updateClocks(remainingSeconds: number) {
    this.remainingSeconds = remainingSeconds;
    let encodedValue = this.getEncodedValue();

    if (this.previousEncodedValue == encodedValue) {
      return;
    }

    this.control(ControlValue.Clock, encodedValue);

    this.previousEncodedValue = encodedValue;
  }

  public updateSemaphores(semaphoreLight: SemaphoreLight) {
    this.semaphoreLight = semaphoreLight;
    this.control(ControlValue.Semaphore, this.getEncodedValue());
  }

  public updateLines(lines: LineOrder) {
    this.control(ControlValue.Lines, lines);
  }

  private initAudio() {
    var audio = new Audio();
    audio.src = "buzzer.wav";
    audio.preload = "auto";
    audio.load();
    return audio;
  }

  public playAudio(count: number) {
    if (this.settings.soundEnabled) {
      var self = this;
      var handler = function () {
        count--;
        if (count > 0) {
          self.audio.play();
        } else {
          self.audio.removeEventListener("ended", handler);
        }
      };
      this.audio.addEventListener("ended", handler, false);
      this.audio.play();
    }
  }

  public playSiren(count: number) {
    this.playAudio(count);
    this.control(ControlValue.Siren, count);
  }

  public countdown(count: number, start: boolean) {
    let encodedValue =
      count |
      (start ? 1 << 16 : 0) |
      (this.settings.brightness << 24);
    this.control(ControlValue.Countdown, encodedValue);
  }

  private control(control: ControlValue, value: number) {
    this.getClients(CONTROL_CAPABILITY[control]).forEach((address) => {
      this.http
        .post("http://" + address + "/control", {
          control: control,
          value: value,
        })
        .subscribe();
    });
  }
}
