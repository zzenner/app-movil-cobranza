import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';
import { roleGuard } from './role.guard';
import { AuthService } from '../auth/auth.service';

const fakeState = {} as RouterStateSnapshot;

function makeRoute(roles: string[]): ActivatedRouteSnapshot {
  return { data: { roles } } as unknown as ActivatedRouteSnapshot;
}

const adminProfile = {
  usuarioId: 'u1', sesionId: 's1', dispositivoId: null, tipoCliente: 'WEB',
  nombreUsuario: 'admin', roles: ['ADMIN'], permisos: [],
};

describe('roleGuard', () => {
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
    authService.markAsAuthenticated(adminProfile, 'tok');
  });

  it('permite acceso cuando el usuario tiene el rol requerido', async () => {
    const result = await TestBed.runInInjectionContext(() =>
      firstValueFrom(roleGuard(makeRoute(['ADMIN']), fakeState) as Observable<boolean | UrlTree>),
    );
    expect(result).toBe(true);
  });

  it('redirige a /forbidden cuando el usuario no tiene el rol requerido', async () => {
    const result = await TestBed.runInInjectionContext(() =>
      firstValueFrom(roleGuard(makeRoute(['SUPERVISOR']), fakeState) as Observable<boolean | UrlTree>),
    );
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/forbidden');
  });

  it('permite acceso cuando no se requiere rol específico', async () => {
    const result = await TestBed.runInInjectionContext(() =>
      firstValueFrom(roleGuard(makeRoute([]), fakeState) as Observable<boolean | UrlTree>),
    );
    expect(result).toBe(true);
  });

  it('redirige a /login cuando NO_AUTENTICADA', async () => {
    authService.markAsUnauthenticated();

    const result = await TestBed.runInInjectionContext(() =>
      firstValueFrom(roleGuard(makeRoute(['ADMIN']), fakeState) as Observable<boolean | UrlTree>),
    );
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/login');
  });
});
