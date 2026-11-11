import { Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { Subscription, concatMap, from, timeout, concatWith } from "rxjs";

import { Settings, LineOrder, SemaphoreLight } from "./settings";
import { MainComponentEventBus } from './main/main.component';

const DEVICE_PORT = 80;

enum ControlValue {
  Clock = 1,
  Semaphore = 2,
  Lines = 3,
  Siren = 4,
  Countdown = 5,
  Display = 6,
  Time = 7
}

const CONTROL_CAPABILITY: Record<ControlValue, string> = {
  [ControlValue.Clock]: "clock",
  [ControlValue.Semaphore]: "semaphore",
  [ControlValue.Lines]: "lines",
  [ControlValue.Siren]: "siren",
  [ControlValue.Countdown]: "countdown",
  [ControlValue.Display]: "display",
  [ControlValue.Time]: "time"
};

export type RgbImage = {
  width: number;
  height: number;
  data: Uint8Array;
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
      .post(`http://${address}:${DEVICE_PORT}/capabilities`, {})
      .pipe(timeout(20000))
      .subscribe({
        next(response: any) {
          self.updateCapabilitiesMap(address, response.capabilities.toString());
          self.scanProcess.delete(address);
          self.updateProgress();
        },
        error(error) {
          console.warn(`Capability request failed for ${address}`, error);
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

  public showTime() {
    const now = new Date();
    const hours = now.getHours();
    const minutes = now.getMinutes();
    this.getClients(CONTROL_CAPABILITY[ControlValue.Time]).forEach((address) => {
      this.http.post(`http://${address}:${DEVICE_PORT}/control`, {
        control: ControlValue.Time,
        hours: hours,
        minutes: minutes,
        brightness: this.settings.brightness
      }).subscribe();
    });
  }

  public store(image: RgbImage) {
    this.getClients(CONTROL_CAPABILITY[ControlValue.Display]).forEach((address) => {
      const tiles: Array<{ tileX: number; tileY: number }> = [];
      for (let tileY = 0; tileY < image.height; tileY += 16) {
        for (let tileX = 0; tileX < image.width; tileX += 16) {
          tiles.push({ tileX, tileY });
        }
      }

      from(tiles)
        .pipe(
          concatMap(({ tileX, tileY }) => {
            const encoded = this.encodeTile(image, tileX, tileY);
            return this.http.post(`http://${address}:${DEVICE_PORT}/control`, {
              control: ControlValue.Display,
              image: encoded,
            });
          }),
          concatWith(
          this.http.post(`http://${address}:${DEVICE_PORT}/control`, {
            control: ControlValue.Display,
            brightness: this.settings.brightness
          }))
        )
        .subscribe({
          next: () => console.log("Image tile stored:", address),
          error: (error) => console.error("Image tile store failed:", address, error),
          complete: () => console.log("All image tiles stored:", address),
        });
    });
  }

  public show() {
    this.getClients(CONTROL_CAPABILITY[ControlValue.Display]).forEach((address) => {
      this.http.post(`http://${address}:${DEVICE_PORT}/control`, {
        control: ControlValue.Display,
        brightness: this.settings.brightness
      }).subscribe({
        next: () => console.log("Stored image shown:", address),
        error: (error) => console.error("Stored image show failed:", address, error),
      });
    });
  }

  private encodeTile(image: RgbImage, tileX: number, tileY: number): string {
    const tileData = new Uint8Array(16 * 16 * 3);
    for (let y = 0; y < 16; y++) {
      for (let x = 0; x < 16; x++) {
        const sourceX = tileX + x;
        const sourceY = tileY + y;
        const targetOffset = (y * 16 + x) * 3;
        if (sourceX < image.width && sourceY < image.height) {
          const sourceOffset = (sourceY * image.width + sourceX) * 3;
          tileData[targetOffset] = image.data[sourceOffset] ?? 0;
          tileData[targetOffset + 1] = image.data[sourceOffset + 1] ?? 0;
          tileData[targetOffset + 2] = image.data[sourceOffset + 2] ?? 0;
        }
      }
    }
    return `16x16@${tileX},${tileY}:${this.toBase64(tileData)}`;
  }

  private toBase64(data: Uint8Array): string {
    let binary = "";
    const chunk = 0x8000;
    for (let i = 0; i < data.length; i += chunk) {
      binary += String.fromCharCode(...data.subarray(i, i + chunk));
    }
    return btoa(binary);
  }

  private control(control: ControlValue, value: number) {
    this.getClients(CONTROL_CAPABILITY[control]).forEach((address) => {
      this.http
        .post(`http://${address}:${DEVICE_PORT}/control`, {
          control: control,
          value: value,
        })
        .subscribe();
    });
  }
}
