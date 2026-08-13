import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ImportacionService } from './importacion.service';

const BASE = '/api/v1/admin/importaciones/mensuales';
const BASE_CARTERAS = '/api/v1/admin/carteras';

const mockResumen = {
  id: 'imp-001',
  carteraId: 'cart-001',
  periodo: '2026-08',
  sistemaOrigen: 'LEGADO',
  estado: 'VALIDADA' as const,
  nombreArchivoOriginal: 'test.csv',
  filasTotales: 5,
  filasProcesadas: null,
  filasRechazadas: 0,
  filasAdvertencia: 0,
  fechaCreacion: '2026-08-09T00:00:00Z',
  fechaActualizacion: '2026-08-09T00:00:00Z',
};

const mockPagina = {
  contenido: [mockResumen],
  pagina: 0,
  tamanio: 20,
  totalElementos: 1,
  totalPaginas: 1,
};

const mockDetalle = {
  ...mockResumen,
  usuarioId: 'usr-001',
  personasCreadas: 2,
  personasActualizadas: 0,
  operacionesCreadas: 2,
  operacionesActualizadas: 0,
  cuotasCreadas: 5,
  cuotasActualizadas: 0,
  mensajeError: null,
  version: 2,
};

const mockErroresPagina = {
  contenido: [
    {
      id: 'err-001',
      numeroFila: 2,
      columna: 'RUT_NUMERO',
      codigoError: 'RUT_INVALIDO_MODULO_11',
      nivel: 'ERROR' as const,
      mensaje: 'RUT inválido',
    },
  ],
  pagina: 0,
  tamanio: 50,
  totalElementos: 1,
  totalPaginas: 1,
};

describe('ImportacionService', () => {
  let service: ImportacionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ImportacionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listarCarteras hace GET /api/v1/admin/carteras/activas', () => {
    service.listarCarteras().subscribe();
    const req = httpMock.expectOne(`${BASE_CARTERAS}/activas`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'cart-001', nombre: 'Cartera Test' }]);
  });

  it('listar hace GET con params de paginación', () => {
    service.listar(0, 20).subscribe();
    const req = httpMock.expectOne((r) => r.url === BASE && r.params.has('pagina'));
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('pagina')).toBe('0');
    expect(req.request.params.get('tamanio')).toBe('20');
    req.flush(mockPagina);
  });

  it('listar incluye carteraId cuando se provee', () => {
    service.listar(0, 20, 'cart-001').subscribe();
    const req = httpMock.expectOne((r) => r.url === BASE && r.params.has('carteraId'));
    expect(req.request.params.get('carteraId')).toBe('cart-001');
    req.flush(mockPagina);
  });

  it('listar NO incluye carteraId cuando no se provee', () => {
    service.listar(0, 20).subscribe();
    const req = httpMock.expectOne((r) => r.url === BASE);
    expect(req.request.params.has('carteraId')).toBe(false);
    req.flush(mockPagina);
  });

  it('obtener hace GET /importaciones/mensuales/:id', () => {
    service.obtener('imp-001').subscribe();
    const req = httpMock.expectOne(`${BASE}/imp-001`);
    expect(req.request.method).toBe('GET');
    req.flush(mockDetalle);
  });

  it('listarErrores hace GET con params de paginación', () => {
    service.listarErrores('imp-001', 0, 50).subscribe();
    const req = httpMock.expectOne((r) => r.url === `${BASE}/imp-001/errores`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('pagina')).toBe('0');
    expect(req.request.params.get('tamanio')).toBe('50');
    req.flush(mockErroresPagina);
  });

  it('confirmar hace POST /importaciones/mensuales/:id/confirmar', () => {
    service.confirmar('imp-001').subscribe();
    const req = httpMock.expectOne(`${BASE}/imp-001/confirmar`);
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('crear envía multipart/form-data con archivo y sistemaOrigen (contrato v2)', () => {
    const archivo = new File(['rut;nombre'], 'test.csv', { type: 'text/csv' });
    service.crear('LEGADO', archivo).subscribe();
    const req = httpMock.expectOne(BASE);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeInstanceOf(FormData);
    const body = req.request.body as FormData;
    expect(body.get('sistemaOrigen')).toBe('LEGADO');
    expect(body.get('archivo')).toBeInstanceOf(File);
    req.flush({ importacionId: 'imp-001', estado: 'RECIBIDA', periodo: null, nombreArchivoOriginal: 'test.csv' });
  });

  it('crear NO incluye carteraId en el FormData (viene en el CSV)', () => {
    const archivo = new File(['rut;nombre'], 'test.csv', { type: 'text/csv' });
    service.crear('LEGADO', archivo).subscribe();
    const req = httpMock.expectOne(BASE);
    const body = req.request.body as FormData;
    expect(body.get('carteraId')).toBeNull();
    req.flush({ importacionId: 'imp-001', estado: 'RECIBIDA', periodo: null, nombreArchivoOriginal: 'test.csv' });
  });

  it('crear NO incluye periodo en el FormData (viene en el CSV)', () => {
    const archivo = new File(['rut;nombre'], 'test.csv', { type: 'text/csv' });
    service.crear('LEGADO', archivo).subscribe();
    const req = httpMock.expectOne(BASE);
    const body = req.request.body as FormData;
    expect(body.get('periodo')).toBeNull();
    req.flush({ importacionId: 'imp-001', estado: 'RECIBIDA', periodo: null, nombreArchivoOriginal: 'test.csv' });
  });
});
