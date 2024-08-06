import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class YearService {
  private yearSource = new BehaviorSubject<number>(new Date().getFullYear());
  currentYear = this.yearSource.asObservable();

  changeYear(year: number) {
    console.log('YearService: changing year to', year); // Debugging

    this.yearSource.next(year);
  }
}