import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppComponent } from './app.component';
import { MapComponent } from './map/map.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { PropertyModalComponent } from './property-modal/property-modal.component';
import { MatDialogModule } from '@angular/material/dialog';
import { HttpClientModule } from '@angular/common/http';
import { MatTreeModule } from '@angular/material/tree';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { GeoJsonService } from './services/geo-json.service';
import { StateService } from './services/state-service.service';
import { MatSliderModule } from '@angular/material/slider';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatNativeDateModule } from '@angular/material/core';
import { MatInputModule } from '@angular/material/input';
import { ReactiveFormsModule } from '@angular/forms'; // or FormsModule if you use template-driven forms
import {MatCheckboxModule} from '@angular/material/checkbox';
import {FormsModule} from '@angular/forms';
import {MatCardModule} from '@angular/material/card';
import { YearSliderComponent } from './year-slider/year-slider.component';
import { YearCalendarComponent } from './year-calendar/year-calendar.component';
import { YearService } from './services/year-service.service';
import { MatRadioModule } from '@angular/material/radio';




@NgModule({
  declarations: [
    AppComponent,
    MapComponent,
    PropertyModalComponent,
    YearSliderComponent,
    YearCalendarComponent
  ],
  imports: [
    BrowserModule,
    MatDialogModule,
    HttpClientModule,
    MatTreeModule,
    MatButtonModule,
    MatIconModule,
    BrowserAnimationsModule,
    MatSliderModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatNativeDateModule,
    MatInputModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
    MatCheckboxModule,
    MatRadioModule,
    MatSliderModule,
    ReactiveFormsModule
  ],
  providers: [GeoJsonService, StateService, YearService],
  bootstrap: [AppComponent]
})
export class AppModule { }
