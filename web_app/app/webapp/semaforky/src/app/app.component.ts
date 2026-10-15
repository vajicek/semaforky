import { Component } from "@angular/core";
import { CommonModule } from "@angular/common";
import { RouterOutlet } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { gitHash, gitDate } from './version';

@Component({
  selector: "app-root",
  standalone: true,
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.css"],
  imports: [
    CommonModule,
    RouterOutlet,
    TranslatePipe
  ]
})
export class AppComponent {
  title = "semaforky";
  gitHash = gitHash;
  gitDate = gitDate;

  constructor(private translate: TranslateService) {
    this.translate.setFallbackLang('en');
    this.translate.use('en');
  }
}
