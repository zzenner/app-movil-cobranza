import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { CarterasService } from './carteras.service';

describe('CarterasService', () => {
  let service: CarterasService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CarterasService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listarCatalogo llama GET /api/v1/admin/carteras', () => {
    service.listarCatalogo().subscribe();
    const req = httpMock.expectOne('/api/v1/admin/carteras');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
