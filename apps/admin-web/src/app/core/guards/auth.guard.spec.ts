import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';
import { authGuard } from './auth.guard';
import { AuthService } from '../auth/auth.service';

const fakeRoute = {} as ActivatedRouteSnapshot;
const fakeState = {} as RouterStateSnapshot;

describe('authGuard', () => {
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

  it('permite acceso cuando AUTENTICADA', async () => {
    authService.markAsAuthenticated(
      { usuarioId: 'u1', sesionId: 's1', dispositivoId: null, tipoCliente: 'WEB',
        nombreUsuario: 'u', roles: [], permisos: [] },
      'tok',
    );

    const result = await TestBed.runInInjectionContext(() =>
      firstValueFrom(authGuard(fakeRoute, fakeState) as Observable<boolean | UrlTree>),
    );
    expect(result).toBe(true);
  });

  it('redirige a /login cuando NO_AUTENTICADA', async () => {
    authService.markAsUnauthenticated();

    const result = await TestBed.runInInjectionContext(() =>
      firstValueFrom(authGuard(fakeRoute, fakeState) as Observable<boolean | UrlTree>),
    );
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/login');
  });
});
