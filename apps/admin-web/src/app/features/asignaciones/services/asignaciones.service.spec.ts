import { describe, it, expect, vi, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AsignacionesService } from './asignaciones.service';

describe('AsignacionesService', () => {
  let service: AsignacionesService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AsignacionesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('listarPeriodos — GET /api/v1/admin/asignaciones/periodos', () => {
    service.listarPeriodos().subscribe((res) => {
      expect(res).toHaveLength(1);
      expect(res[0].periodo).toBe('2026-08');
    });
    http.expectOne('/api/v1/admin/asignaciones/periodos').flush([{ periodo: '2026-08' }]);
  });

  it('listarPeriodos con filtros — agrega query params', () => {
    service.listarPeriodos({ carteraId: 'cid-1' }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/v1/admin/asignaciones/periodos');
    expect(req.request.params.get('carteraId')).toBe('cid-1');
    req.flush([]);
  });

  it('listarMensuales — GET /api/v1/admin/asignaciones/mensuales', () => {
    service.listarMensuales({ periodo: '2026-08' }).subscribe((res) => expect(res).toHaveLength(0));
    http.expectOne((r) => r.url === '/api/v1/admin/asignaciones/mensuales').flush([]);
  });

  it('listarPersonasDisponibles — GET /api/v1/admin/asignaciones/mensuales/:id/personas-disponibles', () => {
    service.listarPersonasDisponibles('mens-id').subscribe();
    http.expectOne('/api/v1/admin/asignaciones/mensuales/mens-id/personas-disponibles').flush([]);
  });

  it('listarDiarias — GET /api/v1/admin/asignaciones/diarias', () => {
    service.listarDiarias({ estado: 'BORRADOR' }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/v1/admin/asignaciones/diarias');
    expect(req.request.params.get('estado')).toBe('BORRADOR');
    req.flush([]);
  });

  it('obtenerDetalle — GET /api/v1/admin/asignaciones/diarias/:id', () => {
    service.obtenerDetalle('diaria-id').subscribe();
    http.expectOne('/api/v1/admin/asignaciones/diarias/diaria-id').flush({});
  });

  it('crearBorrador — POST /api/v1/admin/asignaciones/diarias', () => {
    service.crearBorrador({ asignacionMensualId: 'm-id', fecha: '2026-08-15', personaIds: [] }).subscribe();
    const req = http.expectOne('/api/v1/admin/asignaciones/diarias');
    expect(req.request.method).toBe('POST');
    req.flush({ id: 'nuevo-id' });
  });

  it('actualizarPersonas — PUT /api/v1/admin/asignaciones/diarias/:id/personas', () => {
    service.actualizarPersonas('d-id', { personaIds: ['p-1'] }).subscribe();
    const req = http.expectOne('/api/v1/admin/asignaciones/diarias/d-id/personas');
    expect(req.request.method).toBe('PUT');
    req.flush(null);
  });

  it('publicar — POST /api/v1/admin/asignaciones/diarias/:id/publicar', () => {
    service.publicar('d-id').subscribe();
    const req = http.expectOne('/api/v1/admin/asignaciones/diarias/d-id/publicar');
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('cancelar — POST /api/v1/admin/asignaciones/diarias/:id/cancelar', () => {
    service.cancelar('d-id', { motivo: 'test' }).subscribe();
    const req = http.expectOne('/api/v1/admin/asignaciones/diarias/d-id/cancelar');
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });
});
