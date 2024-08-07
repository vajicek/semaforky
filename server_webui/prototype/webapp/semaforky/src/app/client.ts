import { HttpClient, HttpHeaders } from "@angular/common/http";
import { LineOrder, SemaphoreLight } from "./settings";
import { AppComponent } from "./app.component";

type Request = { control: string; value: string };

export class RestClientController {
  remainingSeconds: number = 0;
  previousEncodedValue: number = 0;
  progress: number = 0;
  audio: any = null;
  semaphoreLight: SemaphoreLight = SemaphoreLight.NONE;

  constructor(private http: HttpClient, private semaforky: AppComponent) {
    this.audio = this.initAudio();
  }

  public getAllClients(): Set<string> {
    var addresses: Set<string> = new Set<string>();
    this.semaforky.settings.clientsByCapability.forEach(
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

  protected updateProgess(increment: number = 0) {
    this.progress += increment;
    this.semaforky.scanEnabled = this.progress == 0;
    if (this.progress == 0) {
      this.semaforky.settings.storeState();
    }
  }

  protected updateCapabilitiesMap(clientAddress: string, capabilities: string) {
    var capabilitiesArray: string[] = capabilities.split(",");
    capabilitiesArray.forEach((capability) => {
      if (this.semaforky.settings.clientsByCapability.has(capability)) {
        this.semaforky.settings.clientsByCapability
          .get(capability)
          ?.push(clientAddress);
      } else {
        this.semaforky.settings.clientsByCapability.set(capability, [
          clientAddress,
        ]);
      }
    });
  }

  protected queryCapabilities(address: string) {
    var self = this;
    this.http
      .post("http://" + address + "/capabilities", {
        headers: new HttpHeaders({ timeout: `${5000}` }),
      })
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

  public scan() {
    this.semaforky.settings.clientsByCapability.clear();
    this.updateProgess(254);
    var network = this.semaforky.settings.network;
    var lastIndex = network.lastIndexOf(".");
    var networkPrefix = network.substring(0, lastIndex + 1);
    for (var i = 1; i < 255; i++) {
      this.queryCapabilities(networkPrefix + i);
    }
  }

  public getClients(capability: string): string[] {
    var retval = this.semaforky.settings.clientsByCapability.get(capability);
    if (retval == undefined) {
      return [];
    }
    return retval;
  }

  protected getEncodedValue() {
    return (
      this.remainingSeconds |
      (this.semaforky.settings.brightness << 24) |
      (Number(this.semaphoreLight) << 16)
    );
  }

  public updateClocks(remainingSeconds: number) {
    this.remainingSeconds = remainingSeconds;
    let encodedValue = this.getEncodedValue();

    if (this.previousEncodedValue == encodedValue) {
      return;
    }

    this.control("clock", 1, encodedValue);

    this.previousEncodedValue = encodedValue;
  }

  public updateSemaphores(semaphoreLight: SemaphoreLight) {
    this.semaphoreLight = semaphoreLight;
    this.control("semaphore", 2, this.getEncodedValue());
  }

  public updateLines(lines: LineOrder) {
    this.control("lines", 3, lines);
  }

  private initAudio() {
    var audio = new Audio();
    audio.src = "buzzer.wav";
    audio.load();
    return audio;
  }

  public playAudio(count: number) {
    if (this.semaforky.settings.soundEnabled) {
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
    this.control("siren", 4, count);
  }

  public countdown(count: number, start: boolean) {
    let encodedValue =
      count |
      (start ? 1 << 16 : 0) |
      (this.semaforky.settings.brightness << 24);
    this.control("countdown", 5, encodedValue);
  }

  private control(clients: string, control: number, value: number) {
    this.getClients(clients).forEach((address) => {
      this.http
        .post("http://" + address + "/control", {
          control: control,
          value: value,
        })
        .subscribe();
    });
  }
}
