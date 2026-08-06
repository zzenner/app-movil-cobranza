import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { UsuariosService } from './usuarios.service';

const mockListado = {
  contenido: [
    {
      id: '11111111-0000-0000-0000-000000000001',
      nombreUsuario: 'admin.test',
      nombres: 'Admin',
      apellidoPaterno: 'Test',
      apellidoMaterno: null,
      correo: null,
      estadoCalculado: 'ACTIVO',
      bloqueadoHasta: null,
      roles: ['JEFE_SUPERVISORES'],
      supervisorId: null,
      supervisorNombreUsuario: null,
      fechaCreacion: '2026-08-01T00:00:00Z',
    },
  ],
  pagina: 0,
  tamanio: 20,
  totalElementos: 1,
  totalPaginas: 1,
};

const mockDetalle = {
  id: '11111111-0000-0000-0000-000000000001',
  nombreUsuario: 'admin.test',
  nombres: 'Admin',
  apellidoPaterno: 'Test',
  apellidoMaterno: null,
  correo: null,
  estadoCalculado: 'ACTIVO',
  activo: true,
  bloqueado: false,
  bloqueadoHasta: null,
  intentosFallidos: 0,
  fechaUltimoAcceso: null,
  roles: [{ codigo: 'JEFE_SUPERVISORES', fechaAsignacion: '2026-08-01T00:00:00Z' }],
  permisosEfectivos: ['USUARIOS_VER'],
  supervisorId: null,
  supervisorNombreUsuario: null,
  fechaCreacion: '2026-08-01T00:00:00Z',
  fechaActualizacion: '2026-08-01T00:00:00Z',
};

describe('UsuariosService', () => {
  let service: UsuariosService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UsuariosService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listar sin filtros envía GET /api/v1/admin/usuarios con pagina y tamanio', () => {
    service.listar(0, 20, {}).subscribe((r) => {
      expect(r.contenido).toHaveLength(1);
    });
    const req = httpMock.expectOne((r) => r.url === '/api/v1/admin/usuarios');
    expect(req.request.params.get('pagina')).toBe('0');
    expect(req.request.params.get('tamanio')).toBe('20');
    expect(req.request.params.has('nombreUsuario')).toBe(false);
    req.flush(mockListado);
  });

  it('listar con nombreUsuario envía el parámetro', () => {
    service.listar(0, 20, { nombreUsuario: 'admin' }).subscribe();
    const req = httpMock.expectOne((r) => r.url === '/api/v1/admin/usuarios');
    expect(req.request.params.get('nombreUsuario')).toBe('admin');
    req.flush(mockListado);
  });

  it('listar con estado envía el parámetro', () => {
    service.listar(0, 20, { estado: 'ACTIVO' }).subscribe();
    const req = httpMock.expectOne((r) => r.url === '/api/v1/admin/usuarios');
    expect(req.request.params.get('estado')).toBe('ACTIVO');
    req.flush(mockListado);
  });

  it('listar con rol envía el parámetro', () => {
    service.listar(0, 20, { rol: 'SUPERVISOR' }).subscribe();
    const req = httpMock.expectOne((r) => r.url === '/api/v1/admin/usuarios');
    expect(req.request.params.get('rol')).toBe('SUPERVISOR');
    req.flush(mockListado);
  });

  it('listar con nombreUsuario en blanco no envía el parámetro', () => {
    service.listar(0, 20, { nombreUsuario: '   ' }).subscribe();
    const req = httpMock.expectOne((r) => r.url === '/api/v1/admin/usuarios');
    expect(req.request.params.has('nombreUsuario')).toBe(false);
    req.flush(mockListado);
  });

  it('obtenerDetalle envía GET /api/v1/admin/usuarios/:id', () => {
    const id = '11111111-0000-0000-0000-000000000001';
    service.obtenerDetalle(id).subscribe((r) => {
      expect(r.nombreUsuario).toBe('admin.test');
    });
    const req = httpMock.expectOne(`/api/v1/admin/usuarios/${id}`);
    expect(req.request.method).toBe('GET');
    req.flush(mockDetalle);
  });
});
