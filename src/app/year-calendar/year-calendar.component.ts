import { Component, OnInit } from '@angular/core';
import { YearService } from '../services/year-service.service';
import { MatCalendar } from '@angular/material/datepicker';

@Component({
  selector: 'app-year-calendar',
  template: `
    <mat-calendar [(selected)]="selectedDate"></mat-calendar>
  `,
  styles: []
})
export class YearCalendarComponent implements OnInit {
  selectedDate: Date = new Date(); 

  constructor(private yearService: YearService) {}

  ngOnInit() {
    this.yearService.currentYear.subscribe(year => {
      console.log('YearCalendarComponent: received year', year); // Debugging

      const currentDate = new Date(this.selectedDate);
      currentDate.setFullYear(year);
      this.selectedDate = currentDate;
    });
  }

  onDateChange(event: Date) {
    console.log('YearCalendarComponent: date changed to', event); // Debugging

    this.yearService.changeYear(event.getFullYear());
  }
}
