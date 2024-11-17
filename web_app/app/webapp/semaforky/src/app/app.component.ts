import { Component } from "@angular/core";
import { CommonModule } from "@angular/common";
import { RouterOutlet } from '@angular/router';
import { TranslateService, TranslateModule } from '@ngx-translate/core';

import { gitHash, gitDate } from './version';

@Component({
  selector: "app-root",
  standalone: true,
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.css"],
  imports: [
    CommonModule,
    RouterOutlet,
    TranslateModule
  ]
})
export class AppComponent {
  gitHash = gitHash;
  gitDate = gitDate;

  constructor(private translate: TranslateService) {
    this.translate.setDefaultLang('en');
  }
}
