import { NgZone, AfterViewChecked, AfterViewInit, ChangeDetectorRef, Component, EventEmitter, OnInit, Output, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, FormControl } from '@angular/forms';
import { MatSlider } from '@angular/material/slider';
import { RdfQueryService } from '../services/rdf-query.service';
import { StateService } from '../services/state-service.service';
import { FlatTreeControl } from '@angular/cdk/tree';
import { MatTreeFlatDataSource, MatTreeFlattener } from '@angular/material/tree';
import { Observable, debounceTime } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { PropertyModalComponent } from '../property-modal/property-modal.component';
import { GeoJsonService } from '../services/geo-json.service';
import { State } from '../models/state.model';
import * as L from 'leaflet';

interface TreeNode {
  stateName: string;
  countyName?: string; // Optional for states
  children?: TreeNode[];
}

interface FlatNode {
  expandable: boolean;
  name: string;
  level: number;
  stateName: string;
  countyName?: string; // Optional for states
}

@Component({
  selector: 'app-map',
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.css']
})
export class MapComponent implements OnInit,  AfterViewChecked {

  @ViewChild(MatSlider) slider!: MatSlider;
  sliderControl = new FormControl(50);

  searchForm: FormGroup;
  searchResults: any[] = [];  // Ensure it's initialized as an empty array
  previousResults: any[] = [];
  errorMessage: string | null = null;
  suggestions: string[] = [];
  showPreviousMap: boolean = false; // Flag to control visibility of the previous map

  private map: L.Map | null = null;
  previousMap: L.Map | null = null;

  states: State[] = [];
  private drawnLayers: L.LayerGroup | null = null; // Store drawn layers for removal
  private previousdrawnLayers: L.LayerGroup | null = null; // Store drawn layers for removal
  metadata: any = {};
  previousMetadata: any = {};
  previousVersion: any = {};
  minYear = 1600;
  maxYear = 2000;
  currentYear = new Date().getFullYear();
  selectedLocation: string | null = null;
  locationType: string | null = null; // Track whether the selected location is a state or county
  selectedState: string | null = null; // Store selected state
  disabled = false;
  max = 2000;
  min = 1600;
  showTicks = false;
  step = 1;
  thumbLabel = false;
  value = this.sliderControl.value;

  treeControl = new FlatTreeControl<FlatNode>(node => node.level, node => node.expandable);

  treeFlattener = new MatTreeFlattener<TreeNode, FlatNode>(
    (node: TreeNode, level: number) => ({
      expandable: !!node.children && node.children.length > 0,
      name: node.countyName || node.stateName, // Use county name if present, else state name
      level: level,
      stateName: node.stateName,
      countyName: node.countyName
    }),
    node => node.level,
    node => node.expandable,
    node => node.children
  );
  
  dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);
  date = new FormControl(new Date()); // Ensures `date` is always initialized with a `Date` object.

  constructor(
    private fb: FormBuilder,
    private rdfQueryService: RdfQueryService,
    private stateService: StateService,
    private dialog: MatDialog,
    private cdRef: ChangeDetectorRef,
    private ngZone: NgZone,
    private geoJsonService: GeoJsonService
  ) {
    this.searchForm = this.fb.group({
      searchText: [''],
      date: [new Date()],
      slider: [this.currentYear],
      selectedLocation: [null]
    });
  }

  ngOnInit(): void {
    this.initMap();

    this.sliderControl.valueChanges.pipe(debounceTime(1000)).subscribe(() => {
      this.getSliderValue();
    });
    this.stateService.getStates().subscribe(states => {
      this.states = states;
      this.dataSource.data = states.map(state => ({
        stateName: state.name,
        children: state.counties.map(county => ({ stateName: state.name, countyName: county.name }))
      }));
    }, error => {
      console.error('Error fetching states:', error);
    });
    this.searchForm.get('date')!.valueChanges.subscribe(newDate => {
      if (newDate instanceof Date) {
        this.currentYear = newDate.getFullYear();
        this.searchForm.get('slider')!.setValue(this.currentYear, { emitEvent: false });
      }
    });

    this.searchForm.get('slider')!.valueChanges.subscribe(newYear => {
      this.currentYear = newYear;
      const current: Date = this.searchForm.get('date')!.value || new Date();
      const updatedDate = new Date(current.setFullYear(newYear));
      this.searchForm.get('date')!.setValue(updatedDate, { emitEvent: false });
    });

    // Subscribe to searchText value changes to call checkSearchText
    this.searchForm.get('searchText')!.valueChanges.pipe(debounceTime(500)).subscribe(searchText => {
      this.checkSearchText(searchText);
      this.updateSuggestions(searchText);
    });
  }

  ngAfterViewInit(): void {
    if (this.showPreviousMap) {
      setTimeout(() => {
        this.initPreviousMap();
      });
    }
  }

  ngAfterViewChecked(): void {
    // Ensure the previous map is initialized only after the view has been fully checked
    if (this.showPreviousMap ) {
      setTimeout(() => {
        this.initPreviousMap();
        
      });
    }
  }

  getSliderValue() {
    console.log(this.sliderControl.value); // Access the slider value from the form control
  }

  hasChild = (_: number, node: FlatNode) => node.expandable;

  private initMap(): void {
    this.map = L.map('map', {
      center: [40.7128, -74.0060],
      zoom: 10
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap'
    }).addTo(this.map);

    this.drawnLayers = L.layerGroup().addTo(this.map); // Initialize the layer group

    
  }

  private initPreviousMap(): void {
    if (this.previousMap == null) {
      this.previousMap = L.map('previousMap', {
        center: [40.7128, -74.0060],
        zoom: 10
      });
      

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '© OpenStreetMap'
      }).addTo(this.previousMap);

      this.previousdrawnLayers = L.layerGroup().addTo(this.previousMap); // Initialize the layer group
    
    }
  }

  

  

  
  formatLabel(value: number): string {
    return `${value}`;
  }

  onRadioChange(event: any): void {
    const { stateName, countyName } = event.value;
    if (stateName) {
      this.selectedLocation = countyName || stateName;
      this.selectedState = stateName;
      console.log('Selected state:', this.selectedState);
      this.locationType = countyName ? this.selectedState : 'state';
      this.searchForm.get('selectedLocation')!.setValue(this.selectedLocation);
      this.errorMessage = null; // Clear error message when a valid location is selected
    }
  }

  checkSearchText(searchText: string): void {
    if (searchText) {
      const matchingState = this.states.find(state => state.name.toLowerCase() === searchText.toLowerCase());
      if (matchingState) {
        this.selectedLocation = matchingState.name;
        this.locationType = 'state';
        this.searchForm.get('selectedLocation')!.setValue(this.selectedLocation);
        this.errorMessage = null; // Clear error message
        return;
      }
      for (let state of this.states) {
        const matchingCounty = state.counties.find(county => county.name.toLowerCase() === searchText.toLowerCase());
        if (matchingCounty) {
          this.selectedLocation = matchingCounty.name;
          this.locationType = state.name ;
          this.searchForm.get('selectedLocation')!.setValue(this.selectedLocation);
          this.errorMessage = null; // Clear error message
          return;
        }
      }
    }
    this.selectedLocation = null;
    this.locationType = null;
    this.errorMessage = 'No matching state or county found'; // Set error message
  }

  updateSuggestions(searchText: string): void {
    if (searchText) {
      const lowerCaseSearchText = searchText.toLowerCase();
      const stateSuggestions = this.states
        .filter(state => state.name.toLowerCase().includes(lowerCaseSearchText))
        .map(state => state.name);
      const countySuggestions = this.states
        .flatMap(state => state.counties)
        .filter(county => county.name.toLowerCase().includes(lowerCaseSearchText))
        .map(county => county.name);
      this.suggestions = [...stateSuggestions, ...countySuggestions];
    } else {
      this.suggestions = [];
    }
  }

  selectSuggestion(suggestion: string): void {
    this.searchForm.get('searchText')!.setValue(suggestion, { emitEvent: false });
    this.checkSearchText(suggestion);
    this.suggestions = [];

    // Find the corresponding node in the tree data
    const selectedNode = this.treeControl.dataNodes.find(node => node.name === suggestion);

    if (selectedNode) {
      // Ensure the parent node is expanded if necessary
      if (selectedNode.level > 0) {
        const parentNode = this.treeControl.dataNodes.find(node => node.stateName === selectedNode.stateName && node.level === 0);
        if (parentNode) {
          this.treeControl.expand(parentNode);
        }
      }

      // Manually set the form control value and trigger onRadioChange
      this.searchForm.get('selectedLocation')!.setValue(selectedNode);
      this.selectedLocation = selectedNode.countyName || selectedNode.stateName;
      this.selectedState = selectedNode.stateName;
      this.locationType = selectedNode.countyName ? 'county' : 'state';

      // Force the radio button to be selected
      this.treeControl.expand(selectedNode);
      this.cdRef.detectChanges(); // Trigger change detection
      this.onRadioChange({ value: { stateName: selectedNode.stateName, countyName: selectedNode.countyName } });
    }
  }

  private updateMapWithGeoJson(geoJson: any): void {
    if (this.map && this.drawnLayers) {
      this.drawnLayers.clearLayers(); // Clear previous layers
      L.geoJSON(geoJson, {
        style: () => ({
          color: 'blue',
          fillColor: 'cyan',
          fillOpacity: 0.5,
        }),
        
      }).addTo(this.drawnLayers);
    }
  }

  private updatePreviousMapWithGeoJson(geoJson: any): void {
    if (this.previousMap && this.previousdrawnLayers) {
      this.previousdrawnLayers.clearLayers(); // Clear previous layers
      L.geoJSON(geoJson, {
        style: () => ({
          color: 'blue',
          fillColor: 'cyan',
          fillOpacity: 0.5,
        }),
       
      }).addTo(this.previousdrawnLayers);
    }
  }

  onSubmit(): void {
    
    if (this.selectedLocation && this.locationType && this.date.value instanceof Date) {
      const dateString = this.fixDateForBackend(this.date.value);
      this.rdfQueryService.search(dateString, this.selectedLocation, this.locationType).subscribe(
        results => {
          if (results ) {
            this.updateMapWithGeoJson(results);
          }
          else if (this.drawnLayers){
            this.drawnLayers.clearLayers();
          }
          this.searchResults = results || [];
          console.log('Search results:', this.searchResults);
          if (this.selectedLocation && this.locationType){
            this.rdfQueryService.getPreviousCountyVersion(dateString, this.selectedLocation, this.locationType).subscribe(
              previousVersion => {
                if (previousVersion) {
                  this.ngZone.run(() => {
                      this.showPreviousMap = true;
                  }); // Trigger change detection
                  console.log('check ');
                  this.previousVersion = previousVersion || {};
                  setTimeout(() => {
                    this.updatePreviousMapWithGeoJson(previousVersion);
                  }, 100);
                  console.log('Previous version after update:', previousVersion);
                }
                else if (this.previousdrawnLayers){
                  this.previousdrawnLayers.clearLayers();
                }
              },
              error => {
                console.error('Error fetching previous version:', error);
              }
            );
            this.rdfQueryService.getMetadata(dateString, this.selectedLocation, this.locationType).subscribe(
              metadata => {
                this.metadata = metadata || [];
                this.ngZone.run(() => this.metadata = metadata || []);
                this.cdRef.detectChanges(); // Trigger change detection
                console.log('Metadata:', metadata);
              },
              error => {
                console.error('Error fetching metadata:', error); 
              }
            );
            this.cdRef.detectChanges();
                
          }
        },
        error => {
          console.error('Error during search:', error);
        }
      );
    } else {
      this.errorMessage = 'Please select a valid state or county';
    }
  }

  fixDateForBackend(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0'); // Months are zero-indexed
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  @Output() valueChange = new EventEmitter<number>();
  setYear(data: number): void {
    this.sliderControl.setValue(data); // Update slider control without emitting an event
    console.log('Year changed:', this.sliderControl.value);
    this.setDatefromSlider(data);
  }

  setDatefromSlider(year: number): void {
    const current: Date = this.date.value || new Date();
    const updatedDate = new Date(current.setFullYear(year));
    this.date.setValue(updatedDate, { emitEvent: true });
    this.valueChange.emit(year); // Emitting year change for external listeners
    console.log('Slider changed date to:', updatedDate);
  }

  setDate(event: any): void {
    const newDate = event.value;
    if (newDate instanceof Date) {
      this.currentYear = newDate.getFullYear(); // Update the slider to reflect the date change
      this.sliderControl.setValue(this.currentYear, { emitEvent: false }); // Update slider control without emitting an event
      console.log('Date changed:', this.sliderControl.value);
    }
  }

  getCurrentDate(): Date {
    return this.date.value || new Date();
  }

  getCurrentCalendarDate() {
    console.log('Current selected date from calendar:', this.getCurrentDate());
  }

  onSliderChange(event: any): void {
    console.log('Slider changed:', event.value);
    this.sliderControl.setValue(this.currentYear, { emitEvent: false }); // Update slider control without emitting an event
  }
}
