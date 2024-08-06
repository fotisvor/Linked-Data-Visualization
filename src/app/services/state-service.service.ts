import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { State, County } from '../models/state.model';

@Injectable({
  providedIn: 'root'
})
export class StateService {
  private apiUrl = 'http://localhost:8080/states'; // URL to web API

  constructor(private http: HttpClient) {}

  getStates(): Observable<State[]> {
    return this.http.get<any[]>(this.apiUrl).pipe(
      map(data => data.map(item => new State(
        item.name,
        item.counties.map((countyName: string) => new County(countyName))
      )))
    );
  }
}
