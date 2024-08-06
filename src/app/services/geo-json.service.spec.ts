import { TestBed } from '@angular/core/testing';

import { GeoJsonService } from './geo-json.service';

describe('GeoJsonService', () => {
  let service: GeoJsonService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GeoJsonService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
