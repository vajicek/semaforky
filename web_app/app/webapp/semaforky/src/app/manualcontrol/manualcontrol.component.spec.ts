import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ManualControlComponent } from './manualcontrol.component';

describe('ManualcontrolComponent', () => {
  let component: ManualControlComponent;
  let fixture: ComponentFixture<ManualControlComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManualControlComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ManualControlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
