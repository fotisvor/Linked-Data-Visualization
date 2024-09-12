import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
  selector: 'app-property-modal',
  templateUrl: './property-modal.component.html',
  styleUrls: ['./property-modal.component.css']
})
export class PropertyModalComponent {
  constructor(@Inject(MAT_DIALOG_DATA) public data: any) { }
}

