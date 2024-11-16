import { Component } from "@angular/core";
import { CommonModule } from "@angular/common";
import { RouterOutlet } from '@angular/router';

import { gitHash, gitDate } from './version';

@Component({
  selector: "app-root",
  standalone: true,
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.css"],
  imports: [
    CommonModule,
    RouterOutlet
  ]
})
export class AppComponent {
  gitHash = gitHash;
  gitDate = gitDate;
}
