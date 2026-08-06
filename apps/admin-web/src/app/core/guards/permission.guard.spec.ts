import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';
import { permissionGuard } from './permission.guard';
import { AuthService } from '../auth/auth.service';

const fakeState = {} as RouterStateSnapshot;

function makeRoute(permission: string | undefined): ActivatedRouteSnapshot {
  return { data: permission !== undefined ? { permission } : {} } as unknown as ActivatedRouteSnapshot;
}

const profileConPermiso = {
  usuarioId: 'u1', sesionId: 's1', dispositivoId: null, tipoCliente: 'WEB',
  nombreUsuario: 'admin', roles: ['JEFE_SUPERVISORES'], permisos: ['USUARIOS_VER'],
};

const profileSinPermiso = {
  usuarioId: 'u2', sesionId: 's2', dispositivoId: null, tipoCliente: 'WEB',
  nombreUsuario: 'sup', roles: ['SUPERVISOR'], permisos: [],
};

describe('permissionGuard', () => {
  let authService: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    authService = TestBed.inject(AuthService);
  });

  it('permite acceso cuando el usuario tiene el permiso requerido', async () => {
    authService.markAsAuthenticated(profileConPermiso, 'tok');
    const result = await TestBed.runInInjectionContext(() =>
      firstValueFrom(permissionGuard(makeRoute('USUARIOS_VER'), fakeState) as Observable<boolean | UrlTree>),
    );
    expect(result).toBe(true);
  });

  it('redirige a /forbidden cuando el usuario no tiene el permiso requerido', async () => {
    authService.markAsAuthenticated(profileSinPermiso, 'tok');
    const result = await TestBed.runInInjectionContext(() =>
      firstValueFrom(permissionGuard(makeRoute('USUARIOS_VER'), fakeState) as Observable<boolean | UrlTree>),
    );
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/forbidden');
  });

  it('permite acceso cuando no se requiere permiso específico', async () => {
    authService.markAsAuthenticated(profileConPermiso, 'tok');
    const result = await TestBed.runInInjectionContext(() =>
      firstValueFrom(permissionGuard(makeRoute(undefined), fakeState) as Observable<boolean | UrlTree>),
    );
    expect(result).toBe(true);
  });

  it('redirige a /login cuando NO_AUTENTICADA', async () => {
    authService.markAsUnauthenticated();
    const result = await TestBed.runInInjectionContext(() =>
      firstValueFrom(permissionGuard(makeRoute('USUARIOS_VER'), fakeState) as Observable<boolean | UrlTree>),
    );
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/login');
  });

  it('espera a que el bootstrap resuelva (INICIALIZANDO → AUTENTICADA)', async () => {
    const result = firstValueFrom(
      TestBed.runInInjectionContext(() =>
        permissionGuard(makeRoute('USUARIOS_VER'), fakeState) as Observable<boolean | UrlTree>,
      ),
    );
    authService.markAsAuthenticated(profileConPermiso, 'tok');
    expect(await result).toBe(true);
  });
});
