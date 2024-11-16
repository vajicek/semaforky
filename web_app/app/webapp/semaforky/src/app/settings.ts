import { CookieService } from "ngx-cookie-service";
import { Injectable } from '@angular/core';

export enum LinesRotation {
  BYROUND = "BYROUND",
  BYSET = "BYSET",
  NO = "NO"
}

export enum LineOrder {
  AB = 1,
  CD = 2,
  UNDEFINED = 3
}

export enum SemaphoreLight {
  NONE = 0,
  RED = 1,
  GREEN = 2,
  YELLOW = 3
}

@Injectable({
  providedIn: 'root'
})
export class Settings {
  public language: number = 0;
  public roundSets: number = 10;
  public setTime: number = 120;
  public customSetTime: number = 120;
  public preparationTime: number = 10;
  public warningTime: number = 30;
  public lines: number = 1;
  public continuous: boolean = false;
  public numberOfSets: number = 10;
  public linesRotation: LinesRotation = LinesRotation.BYROUND;
  public delayedStartTime: string = "12:00:00";
  public brightness: number = 30;
  public network: string = "192.168.4.0";
  public soundEnabled: boolean = false;
  public clientsByCapability: Map<string, string[]> = new Map<string, string[]>();

  constructor(protected cookieService: CookieService) {
  }

  public getDelayedStartTime(): Date {
    return new Date(new Date().toDateString() + " " + this.delayedStartTime);
  }

  public setCookieValue(key: string, value: string) {
    this.cookieService.set(key, value);
  }

  public getCookieValue(key: string, defaultValue: string): string {
    var value = this.cookieService.get(key);
    return value ? value : defaultValue;
  }

  private set(key: string, value: string) {
    this.setCookieValue(key, value);
  }

  private get(key: string, defaultValue: string): string {
    return this.getCookieValue(key, defaultValue);
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
    this.delayedStartTime = this.get("delayedStartTime", this.delayedStartTime);
    this.linesRotation = (<any>LinesRotation)[this.get("linesRotation", this.linesRotation.toString())];
    this.brightness = parseInt(this.get("brightness", this.brightness.toString()));
    this.network = this.get("network", this.network)
    this.soundEnabled = this.get("soundEnabled", this.soundEnabled.toString()) === "true";
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
    this.set("delayedStartTime", this.delayedStartTime);
    this.set("linesRotation", (<any>LinesRotation)[this.linesRotation]);
    this.set("brightness", this.brightness.toString());
    this.set("network", this.network);
    this.set("soundEnabled", this.soundEnabled.toString());
    this.set("clientsByCapability", JSON.stringify(Array.from(this.clientsByCapability.entries())));
  }
};
