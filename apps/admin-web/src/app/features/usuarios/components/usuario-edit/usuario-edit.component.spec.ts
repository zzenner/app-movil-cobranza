import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { type Mocked } from 'vitest';
import { UsuarioEditComponent } from './usuario-edit.component';
import { UsuariosService } from '../../services/usuarios.service';

const mockDetalle = {
  id: 'u2',
  nombreUsuario: 'operador.test',
  nombres: 'Carlos',
  apellidoPaterno: 'Martinez',
  apellidoMaterno: 'Lopez',
  correo: 'carlos@example.com',
  estadoCalculado: 'ACTIVO' as const,
  activo: true,
  bloqueado: false,
  bloqueadoHasta: null,
  intentosFallidos: 0,
  fechaUltimoAcceso: null,
  roles: [],
  permisosEfectivos: [],
  supervisorId: null,
  supervisorNombreUsuario: null,
  fechaCreacion: '2026-01-01T00:00:00Z',
  fechaActualizacion: '2026-08-01T00:00:00Z',
  version: 3,
};

function crearFixture(serviceMock: Mocked<UsuariosService>) {
  TestBed.configureTestingModule({
    imports: [UsuarioEditComponent],
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
            paramMap: { get: () => 'u2' },
            queryParams: {},
          },
        },
      },
    ],
  });
  return TestBed.createComponent(UsuarioEditComponent);
}

describe('UsuarioEditComponent', () => {
  let serviceMock: Mocked<UsuariosService>;

  beforeEach(() => {
    serviceMock = {
      obtenerDetalle: vi.fn().mockReturnValue(of(mockDetalle)),
      actualizarDatosBasicos: vi.fn(),
    } as unknown as Mocked<UsuariosService>;
  });

  afterEach(() => TestBed.resetTestingModule());

  it('carga y rellena el formulario con los datos del usuario', async () => {
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    const comp = fixture.componentInstance;
    expect(comp.nombreUsuario()).toBe(mockDetalle.nombreUsuario);
    expect(comp.form.value.nombres).toBe(mockDetalle.nombres);
    expect(comp.form.value.apellidoPaterno).toBe(mockDetalle.apellidoPaterno);
    expect(comp.cargando()).toBe(false);
  });

  it('nombreUsuario es de solo lectura (no está en el formulario reactivo)', async () => {
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.componentInstance.form.contains('nombreUsuario')).toBe(false);
    expect(fixture.componentInstance.nombreUsuario()).toBe(mockDetalle.nombreUsuario);
  });

  it('guardar() envía la versión capturada al cargar el detalle', async () => {
    serviceMock.actualizarDatosBasicos.mockReturnValue(of(undefined));
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.componentInstance.guardar();
    await fixture.whenStable();
    expect(serviceMock.actualizarDatosBasicos).toHaveBeenCalledWith(
      'u2',
      expect.objectContaining({ version: mockDetalle.version }),
    );
  });

  it('guardar() navega a /usuarios/:id tras guardar con éxito', async () => {
    serviceMock.actualizarDatosBasicos.mockReturnValue(of(undefined));
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    const router = TestBed.inject(Router);
    const spy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.componentInstance.guardar();
    await fixture.whenStable();
    expect(spy).toHaveBeenCalledWith(['/usuarios', 'u2']);
  });

  it('guardar() muestra mensaje CONFLICTO_VERSION al recibir ese código de error', async () => {
    serviceMock.actualizarDatosBasicos.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { code: 'CONFLICTO_VERSION' },
          }),
      ),
    );
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.componentInstance.guardar();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.componentInstance.errorGeneral()).toBe(
      'Los datos fueron modificados por otro administrador. Vuelva a cargar la página.',
    );
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="error-general"]')).toBeTruthy();
  });

  it('muestra pantalla de error 404 cuando el usuario no existe', async () => {
    serviceMock.obtenerDetalle.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.componentInstance.error404()).toBe(true);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="error-404"]')).toBeTruthy();
  });

  it('guardar() muestra error genérico para errores desconocidos', async () => {
    serviceMock.actualizarDatosBasicos.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: { code: 'OTRO_ERROR' } })),
    );
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.componentInstance.guardar();
    await fixture.whenStable();
    expect(fixture.componentInstance.errorGeneral()).toBe(
      'Error al guardar los cambios. Intente nuevamente.',
    );
  });
});
