import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { SupervisionService } from './supervision.service';

describe('SupervisionService', () => {
  let service: SupervisionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SupervisionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listarEjecutivos llama GET /api/v1/admin/supervision/ejecutivos', () => {
    service.listarEjecutivos().subscribe();
    const req = httpMock.expectOne('/api/v1/admin/supervision/ejecutivos');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('listarSupervisores llama GET /api/v1/admin/supervision/supervisores', () => {
    service.listarSupervisores().subscribe();
    const req = httpMock.expectOne('/api/v1/admin/supervision/supervisores');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('asignarOReaasignar llama POST con supervisorId', () => {
    const ejec = 'uuid-ejec';
    const sup = 'uuid-sup';
    service.asignarOReaasignar(ejec, { supervisorId: sup }).subscribe();
    const req = httpMock.expectOne(`/api/v1/admin/supervision/ejecutivos/${ejec}/supervisor`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ supervisorId: sup });
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('quitarSupervision llama DELETE', () => {
    const ejec = 'uuid-ejec';
    service.quitarSupervision(ejec).subscribe();
    const req = httpMock.expectOne(`/api/v1/admin/supervision/ejecutivos/${ejec}/supervisor`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('actualizarCodigo llama PATCH con codigo', () => {
    const ejec = 'uuid-ejec';
    service.actualizarCodigo(ejec, { codigo: 'EJ-001' }).subscribe();
    const req = httpMock.expectOne(`/api/v1/admin/supervision/ejecutivos/${ejec}/codigo`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ codigo: 'EJ-001' });
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
