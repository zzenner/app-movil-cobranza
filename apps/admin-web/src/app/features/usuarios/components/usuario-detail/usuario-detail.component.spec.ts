import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { type Mocked } from 'vitest';
import { UsuarioDetailComponent } from './usuario-detail.component';
import { UsuariosService } from '../../services/usuarios.service';

const mockDetalle = {
  id: 'u1',
  nombreUsuario: 'admin.test',
  nombres: 'Admin',
  apellidoPaterno: 'Test',
  apellidoMaterno: null,
  correo: null,
  estadoCalculado: 'ACTIVO' as const,
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

function crearFixture(serviceMock: Mocked<UsuariosService>, queryParams = {}) {
  TestBed.configureTestingModule({
    imports: [UsuarioDetailComponent],
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideHttpClientTesting(),
      provideAnimations(),
      { provide: UsuariosService, useValue: serviceMock },
      {
        provide: ActivatedRoute,
        useValue: {
          snapshot: {
            paramMap: { get: () => 'u1' },
            queryParams,
          },
        },
      },
    ],
  });
  return TestBed.createComponent(UsuarioDetailComponent);
}

describe('UsuarioDetailComponent', () => {
  let serviceMock: Mocked<UsuariosService>;

  beforeEach(() => {
    serviceMock = {
      listar: vi.fn(),
      obtenerDetalle: vi.fn().mockReturnValue(of(mockDetalle)),
    } as unknown as Mocked<UsuariosService>;
  });

  afterEach(() => TestBed.resetTestingModule());

  it('carga y muestra el detalle del usuario', async () => {
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.componentInstance.usuario()).toEqual(mockDetalle);
    expect(fixture.componentInstance.cargando()).toBe(false);
  });

  it('muestra 404 cuando el usuario no existe', async () => {
    serviceMock.obtenerDetalle.mockReturnValue(
      throwError(() => ({ status: 404, message: 'not found' })),
    );
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.componentInstance.error404()).toBe(true);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="error-404"]')).toBeTruthy();
  });

  it('muestra error de red para errores != 404/403', async () => {
    serviceMock.obtenerDetalle.mockReturnValue(
      throwError(() => ({ status: 500, message: 'server error' })),
    );
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.componentInstance.errorRed()).toBe(true);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="error-red"]')).toBeTruthy();
  });

  it('redirige a /forbidden para error 403', async () => {
    serviceMock.obtenerDetalle.mockReturnValue(
      throwError(() => ({ status: 403, message: 'forbidden' })),
    );
    const fixture = crearFixture(serviceMock);
    const router = TestBed.inject(Router);
    const spy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(spy).toHaveBeenCalledWith(['/forbidden']);
  });

  it('el botón volver conserva queryParams del listado', async () => {
    const qp = { pagina: '1', estado: 'ACTIVO' };
    const fixture = crearFixture(serviceMock, qp);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.componentInstance.queryParamsVolver).toEqual(qp);
  });
});
