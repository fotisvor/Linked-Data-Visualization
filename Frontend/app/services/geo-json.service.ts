import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class GeoJsonService {

  private geoJsonUrl = 'http://localhost:8080/geojson';  // URL to web api

  constructor(private http: HttpClient) { }

  getGeoJson(): Observable<any> {
    return this.http.get(this.geoJsonUrl);
  }}
