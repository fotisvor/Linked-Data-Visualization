import { Component, OnInit } from '@angular/core';
import { YearService } from '../services/year-service.service';

@Component({
  selector: 'app-year-slider',
  template: `
    <mat-slider
      min="1776"
      max="2024"
      step="1"
      [(ngModel)]="year"
      (change)="onSliderChange($event)"
    ></mat-slider>
  `,
  styles: []
})
export class YearSliderComponent implements OnInit {
  year: number = new Date().getFullYear(); // Initialize with the current year

  constructor(private yearService: YearService) {}

  ngOnInit() {
    this.yearService.currentYear.subscribe(year => {
      console.log('YearSliderComponent: received year', year); // Debugging

      this.year = year;
    });
  }

  onSliderChange(event: any) {
    console.log('YearSliderComponent: slider changed to', event.value); // Debugging

    this.yearService.changeYear(event.value);

  }
}

