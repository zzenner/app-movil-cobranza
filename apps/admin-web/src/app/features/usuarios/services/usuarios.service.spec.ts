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
  version: 0,
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
      expect(r.version).toBe(0);
    });
    const req = httpMock.expectOne(`/api/v1/admin/usuarios/${id}`);
    expect(req.request.method).toBe('GET');
    req.flush(mockDetalle);
  });

  it('listarRoles envía GET /api/v1/admin/roles', () => {
    service.listarRoles().subscribe();
    const req = httpMock.expectOne('/api/v1/admin/roles');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'abc', codigo: 'JEFE_SUPERVISORES', nombre: 'Jefe de Supervisores' }]);
  });

  it('crear envía POST /api/v1/admin/usuarios', () => {
    const solicitud = {
      nombreUsuario: 'nuevo',
      nombres: 'Nuevo',
      apellidoPaterno: 'User',
      contrasena: 'Password123',
      rolesIniciales: ['EJECUTIVO_TERRENO'],
    };
    service.crear(solicitud).subscribe();
    const req = httpMock.expectOne('/api/v1/admin/usuarios');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(solicitud);
    req.flush({ id: '22222222-0000-0000-0000-000000000001' });
  });

  it('actualizarDatosBasicos envía PUT /api/v1/admin/usuarios/:id/datos-basicos', () => {
    const id = '11111111-0000-0000-0000-000000000001';
    const solicitud = { nombres: 'Editado', apellidoPaterno: 'Test', version: 0 };
    service.actualizarDatosBasicos(id, solicitud).subscribe();
    const req = httpMock.expectOne(`/api/v1/admin/usuarios/${id}/datos-basicos`);
    expect(req.request.method).toBe('PUT');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('activar envía POST /api/v1/admin/usuarios/:id/activar', () => {
    const id = '11111111-0000-0000-0000-000000000001';
    service.activar(id).subscribe();
    const req = httpMock.expectOne(`/api/v1/admin/usuarios/${id}/activar`);
    expect(req.request.method).toBe('POST');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('desactivar envía POST /api/v1/admin/usuarios/:id/desactivar', () => {
    const id = '11111111-0000-0000-0000-000000000001';
    service.desactivar(id).subscribe();
    const req = httpMock.expectOne(`/api/v1/admin/usuarios/${id}/desactivar`);
    expect(req.request.method).toBe('POST');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('bloquear envía POST /api/v1/admin/usuarios/:id/bloquear', () => {
    const id = '11111111-0000-0000-0000-000000000001';
    service.bloquear(id).subscribe();
    const req = httpMock.expectOne(`/api/v1/admin/usuarios/${id}/bloquear`);
    expect(req.request.method).toBe('POST');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('desbloquear envía POST /api/v1/admin/usuarios/:id/desbloquear', () => {
    const id = '11111111-0000-0000-0000-000000000001';
    service.desbloquear(id).subscribe();
    const req = httpMock.expectOne(`/api/v1/admin/usuarios/${id}/desbloquear`);
    expect(req.request.method).toBe('POST');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('restablecerContrasena envía POST /api/v1/admin/usuarios/:id/restablecer-contrasena', () => {
    const id = '11111111-0000-0000-0000-000000000001';
    service.restablecerContrasena(id, { nuevaContrasena: 'NuevaClave123' }).subscribe();
    const req = httpMock.expectOne(`/api/v1/admin/usuarios/${id}/restablecer-contrasena`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.nuevaContrasena).toBe('NuevaClave123');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
