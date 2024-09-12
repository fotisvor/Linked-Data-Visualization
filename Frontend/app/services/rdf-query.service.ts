import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class RdfQueryService {
  private apiUrl = 'http://localhost:8080/search';  // Update with your backend URL

  constructor(private http: HttpClient) {}

  search(date: string, selectedLocation: string, locationType: string): Observable<any> {
    const params = new HttpParams()
      .set('date', date)
      .set('selectedLocation', selectedLocation)
      .set('locationType', locationType);
    
    console.log(params);
    return this.http.get<any>(this.apiUrl, { params });
  }

  getPreviousCountyVersion(date: string, selectedLocation: string, locationType: string): Observable<any> {
    const params = new HttpParams()
    .set('date', date)
    .set('selectedLocation', selectedLocation)
    .set('locationType', locationType);
    return this.http.get<any>(`http://localhost:8080/previousCountyVersion`, { params });
  }

  getMetadata(date: string, countyName: string, stateName: string): Observable<any> {
    const params = new HttpParams()
      .set('date', date)
      .set('countyName', countyName)
      .set('stateName', stateName);
  
    return this.http.get<any>(`http://localhost:8080/countyMetadata`, { params });
  }

  getPreviousMetadata(date: string, countyName: string, stateName: string): Observable<any> {
    const params = new HttpParams()
      .set('date', date)
      .set('countyName', countyName)
      .set('stateName', stateName);
    return this.http.get<any>(`http://localhost:8080/previousCountyMetadata`, { params });
  }
}
