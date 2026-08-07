import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { type Mocked } from 'vitest';
import { UsuarioCreateComponent } from './usuario-create.component';
import { UsuariosService } from '../../services/usuarios.service';

const mockRoles = [
  { id: 'r1', codigo: 'ADMINISTRADOR', nombre: 'Administrador' },
  { id: 'r2', codigo: 'JEFE_SUPERVISORES', nombre: 'Jefe de Supervisores' },
];

function crearFixture(serviceMock: Mocked<UsuariosService>) {
  TestBed.configureTestingModule({
    imports: [UsuarioCreateComponent],
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideHttpClientTesting(),
      provideAnimations(),
      { provide: UsuariosService, useValue: serviceMock },
    ],
  });
  return TestBed.createComponent(UsuarioCreateComponent);
}

function completarFormValido(comp: UsuarioCreateComponent) {
  comp.form.setValue({
    nombreUsuario: 'nuevo.usuario',
    nombres: 'Juan',
    apellidoPaterno: 'Perez',
    apellidoMaterno: '',
    correo: '',
    contrasena: 'password123',
  });
  comp.toggleRol('ADMINISTRADOR', true);
}

describe('UsuarioCreateComponent', () => {
  let serviceMock: Mocked<UsuariosService>;

  beforeEach(() => {
    serviceMock = {
      listarRoles: vi.fn().mockReturnValue(of(mockRoles)),
      crear: vi.fn(),
    } as unknown as Mocked<UsuariosService>;
  });

  afterEach(() => TestBed.resetTestingModule());

  it('carga y muestra los roles al inicializar', async () => {
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.componentInstance.roles()).toEqual(mockRoles);
    expect(fixture.componentInstance.cargandoRoles()).toBe(false);
  });

  it('el formulario se renderiza con los campos obligatorios', async () => {
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('input[formControlName="nombreUsuario"]')).toBeTruthy();
    expect(el.querySelector('input[formControlName="nombres"]')).toBeTruthy();
    expect(el.querySelector('input[formControlName="apellidoPaterno"]')).toBeTruthy();
    expect(el.querySelector('input[formControlName="contrasena"]')).toBeTruthy();
  });

  it('guardar() muestra sinRolesError cuando no hay roles seleccionados', async () => {
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    const comp = fixture.componentInstance;
    comp.form.setValue({
      nombreUsuario: 'nuevo.usuario',
      nombres: 'Juan',
      apellidoPaterno: 'Perez',
      apellidoMaterno: '',
      correo: '',
      contrasena: 'password123',
    });
    comp.guardar();
    expect(comp.sinRolesError()).toBe(true);
    expect(serviceMock.crear).not.toHaveBeenCalled();
  });

  it('guardar() navega a /usuarios/:id en creación exitosa', async () => {
    serviceMock.crear.mockReturnValue(of({ id: 'nuevo-id-123' }));
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    const router = TestBed.inject(Router);
    const spy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    completarFormValido(fixture.componentInstance);
    fixture.componentInstance.guardar();
    await fixture.whenStable();
    expect(spy).toHaveBeenCalledWith(['/usuarios', 'nuevo-id-123']);
  });

  it('guardar() muestra error NOMBRE_USUARIO_DUPLICADO al recibir código 409', async () => {
    serviceMock.crear.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { code: 'NOMBRE_USUARIO_DUPLICADO' },
          }),
      ),
    );
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    completarFormValido(fixture.componentInstance);
    fixture.componentInstance.guardar();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.componentInstance.errorGeneral()).toBe('El nombre de usuario ya está en uso.');
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="error-general"]')).toBeTruthy();
  });

  it('el correo es opcional: enviar sin correo es válido y se envía como null', async () => {
    serviceMock.crear.mockReturnValue(of({ id: 'uid-456' }));
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const comp = fixture.componentInstance;
    comp.form.setValue({
      nombreUsuario: 'sin.correo',
      nombres: 'Ana',
      apellidoPaterno: 'Lopez',
      apellidoMaterno: '',
      correo: '',
      contrasena: 'password123',
    });
    comp.toggleRol('ADMINISTRADOR', true);
    expect(comp.form.valid).toBe(true);
    comp.guardar();
    await fixture.whenStable();
    expect(serviceMock.crear).toHaveBeenCalledWith(
      expect.objectContaining({ correo: null }),
    );
  });

  it('sinRolesError se limpia al seleccionar un rol', async () => {
    const fixture = crearFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    const comp = fixture.componentInstance;
    comp.form.setValue({
      nombreUsuario: 'test',
      nombres: 'Test',
      apellidoPaterno: 'User',
      apellidoMaterno: '',
      correo: '',
      contrasena: 'password123',
    });
    comp.guardar();
    expect(comp.sinRolesError()).toBe(true);
    comp.toggleRol('ADMINISTRADOR', true);
    expect(comp.sinRolesError()).toBe(false);
  });
});
