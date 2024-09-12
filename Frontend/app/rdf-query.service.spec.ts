import { TestBed } from '@angular/core/testing';

import { RdfQueryService } from './rdf-query.service';

describe('RdfQueryService', () => {
  let service: RdfQueryService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(RdfQueryService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
