import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { type Mocked } from 'vitest';
import { MatDialog } from '@angular/material/dialog';
import { UsuarioDetailComponent } from './usuario-detail.component';
import { UsuariosService } from '../../services/usuarios.service';
import { AuthService } from '../../../../core/auth/auth.service';
import type { DetalleUsuario } from '../../models/usuario.models';
import type { UserProfile } from '../../../../core/auth/auth.models';

const mockDetalle: DetalleUsuario = {
  id: 'u1',
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

  describe('acciones administrativas — puedeAdministrar y esPropiasCuenta', () => {
    let serviceMockAdmin: Mocked<UsuariosService>;

    const perfilAdmin: UserProfile = {
      usuarioId: 'admin-id-distinto',
      sesionId: 'ses1',
      dispositivoId: null,
      tipoCliente: 'WEB',
      nombreUsuario: 'admin.principal',
      roles: ['ADMINISTRADOR'],
      permisos: ['USUARIOS_ADMINISTRAR', 'USUARIOS_VER'],
    };

    /** Crea el fixture inyectando un AuthService mock. No registra MatDialog en TestBed. */
    function crearFixtureAdmin(
      authProfile: UserProfile | null,
      detalleOverride: Partial<DetalleUsuario> = {},
    ) {
      const detalleMock = { ...mockDetalle, ...detalleOverride };
      serviceMockAdmin.obtenerDetalle.mockReturnValue(of(detalleMock));
      const authMock = { profile: vi.fn().mockReturnValue(authProfile) };
      TestBed.configureTestingModule({
        imports: [UsuarioDetailComponent],
        providers: [
          provideRouter([]),
          provideHttpClient(),
          provideHttpClientTesting(),
          provideAnimations(),
          { provide: UsuariosService, useValue: serviceMockAdmin },
          { provide: AuthService, useValue: authMock },
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                paramMap: { get: () => 'u1' },
                queryParams: {},
              },
            },
          },
        ],
      });
      return TestBed.createComponent(UsuarioDetailComponent);
    }

    /**
     * Reemplaza la propiedad privada `dialog` del componente con un mock después de su
     * construcción. Esto evita que la instancia real de MatDialog (del EnvironmentInjector
     * del componente standalone) interfiera con el test.
     */
    function mockDialog(fixture: ReturnType<typeof crearFixtureAdmin>, closeResult: unknown = true) {
      const ref = { afterClosed: vi.fn().mockReturnValue(of(closeResult)) };
      const mock = { open: vi.fn().mockReturnValue(ref) };
      (fixture.componentInstance as unknown as Record<string, unknown>)['dialog'] = mock;
      return mock;
    }

    beforeEach(() => {
      serviceMockAdmin = {
        listar: vi.fn(),
        obtenerDetalle: vi.fn().mockReturnValue(of(mockDetalle)),
        activar: vi.fn().mockReturnValue(of(undefined)),
        desactivar: vi.fn().mockReturnValue(of(undefined)),
        bloquear: vi.fn().mockReturnValue(of(undefined)),
        desbloquear: vi.fn().mockReturnValue(of(undefined)),
        restablecerContrasena: vi.fn().mockReturnValue(of(undefined)),
      } as unknown as Mocked<UsuariosService>;
    });

    it('muestra la tarjeta de acciones cuando el perfil tiene USUARIOS_ADMINISTRAR', async () => {
      const fixture = crearFixtureAdmin(perfilAdmin);
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="btn-editar"]')).toBeTruthy();
      expect(el.querySelector('[data-testid="btn-reset-password"]')).toBeTruthy();
    });

    it('oculta la tarjeta de acciones cuando el perfil no tiene USUARIOS_ADMINISTRAR', async () => {
      const perfilSinAdmin: UserProfile = { ...perfilAdmin, permisos: ['USUARIOS_VER'] };
      const fixture = crearFixtureAdmin(perfilSinAdmin);
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="btn-editar"]')).toBeFalsy();
      expect(el.querySelector('[data-testid="btn-reset-password"]')).toBeFalsy();
    });

    it('oculta btn-desactivar y btn-bloquear cuando el usuario es la propia cuenta', async () => {
      // mockDetalle.id === 'u1' → si usuarioId === 'u1' entonces esPropiasCuenta() === true
      const perfilPropiasCuenta: UserProfile = { ...perfilAdmin, usuarioId: 'u1' };
      const fixture = crearFixtureAdmin(perfilPropiasCuenta);
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="btn-editar"]')).toBeTruthy();
      expect(el.querySelector('[data-testid="btn-desactivar"]')).toBeFalsy();
      expect(el.querySelector('[data-testid="btn-bloquear"]')).toBeFalsy();
    });

    it('muestra btn-activar cuando el usuario está inactivo', async () => {
      const fixture = crearFixtureAdmin(perfilAdmin, { activo: false, estadoCalculado: 'INACTIVO' });
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="btn-activar"]')).toBeTruthy();
      expect(el.querySelector('[data-testid="btn-desactivar"]')).toBeFalsy();
    });

    it('muestra btn-desbloquear cuando el usuario está bloqueado', async () => {
      const fixture = crearFixtureAdmin(perfilAdmin, { bloqueado: true, estadoCalculado: 'BLOQUEADO' });
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="btn-desbloquear"]')).toBeTruthy();
      expect(el.querySelector('[data-testid="btn-bloquear"]')).toBeFalsy();
    });

    it('accion activar llama a service.activar tras confirmar el diálogo', async () => {
      const fixture = crearFixtureAdmin(perfilAdmin, { activo: false, estadoCalculado: 'INACTIVO' });
      fixture.detectChanges();
      await fixture.whenStable();
      // Sustituir el dialog inyectado ANTES de ejecutar la acción
      mockDialog(fixture, true);
      fixture.componentInstance.accion('activar');
      await fixture.whenStable();
      expect(serviceMockAdmin.activar).toHaveBeenCalledWith('u1');
    });

    it('accion desactivar llama a service.desactivar tras confirmar el diálogo', async () => {
      const fixture = crearFixtureAdmin(perfilAdmin);
      fixture.detectChanges();
      await fixture.whenStable();
      mockDialog(fixture, true);
      fixture.componentInstance.accion('desactivar');
      await fixture.whenStable();
      expect(serviceMockAdmin.desactivar).toHaveBeenCalledWith('u1');
    });

    it('accion bloquear llama a service.bloquear tras confirmar el diálogo', async () => {
      const fixture = crearFixtureAdmin(perfilAdmin);
      fixture.detectChanges();
      await fixture.whenStable();
      mockDialog(fixture, true);
      fixture.componentInstance.accion('bloquear');
      await fixture.whenStable();
      expect(serviceMockAdmin.bloquear).toHaveBeenCalledWith('u1');
    });

    it('abrirResetPassword llama al servicio con la contraseña retornada por el diálogo', async () => {
      const fixture = crearFixtureAdmin(perfilAdmin);
      fixture.detectChanges();
      await fixture.whenStable();
      mockDialog(fixture, 'NuevaPass1!');
      fixture.componentInstance.abrirResetPassword();
      await fixture.whenStable();
      expect(serviceMockAdmin.restablecerContrasena).toHaveBeenCalledWith('u1', {
        nuevaContrasena: 'NuevaPass1!',
      });
    });

    it('no llama a service.activar cuando el diálogo de confirmación se cancela', async () => {
      const fixture = crearFixtureAdmin(perfilAdmin, { activo: false, estadoCalculado: 'INACTIVO' });
      fixture.detectChanges();
      await fixture.whenStable();
      // Pasar false explícitamente (no undefined, que activaría el valor por defecto del helper)
      mockDialog(fixture, false);
      fixture.componentInstance.accion('activar');
      await fixture.whenStable();
      expect(serviceMockAdmin.activar).not.toHaveBeenCalled();
    });
  });
});
