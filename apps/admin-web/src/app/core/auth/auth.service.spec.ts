import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { TokenStorageService } from './token-storage.service';

const mockLoginResp = {
  accessToken: 'at-test',
  expiresInSeconds: 900,
  sessionExpiresAt: '2026-11-01T00:00:00Z',
};

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let tokenStorage: TokenStorageService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    tokenStorage = TestBed.inject(TokenStorageService);
  });

  afterEach(() => httpMock.verify());

  it('should start in INICIALIZANDO state', () => {
    expect(service.sessionState()).toBe('INICIALIZANDO');
  });

  it('should set AUTENTICADA and store token on login', () => {
    service.login({ nombreUsuario: 'admin', clave: '123' }).subscribe();
    const req = httpMock.expectOne('/api/v1/auth/web/login');
    req.flush(mockLoginResp);
    expect(service.sessionState()).toBe('AUTENTICADA');
    expect(tokenStorage.getAccessToken()).toBe('at-test');
  });

  it('should set NO_AUTENTICADA and clear token on logout', () => {
    tokenStorage.setAccessToken('token');
    service.logout().subscribe();
    const req = httpMock.expectOne('/api/v1/auth/web/logout');
    req.flush(null);
    expect(service.sessionState()).toBe('NO_AUTENTICADA');
    expect(tokenStorage.getAccessToken()).toBeNull();
  });

  it('should set NO_AUTENTICADA when refresh fails', () => {
    service.refresh().subscribe({ error: () => {} });
    const req = httpMock.expectOne('/api/v1/auth/web/refresh');
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });
    expect(service.sessionState()).toBe('NO_AUTENTICADA');
    expect(tokenStorage.getAccessToken()).toBeNull();
  });

  it('should complete single-flight: second refresh reuses first observable', () => {
    const results: string[] = [];
    service.refresh().subscribe((r) => results.push(r.accessToken));
    service.refresh().subscribe((r) => results.push(r.accessToken));

    const reqs = httpMock.match('/api/v1/auth/web/refresh');
    expect(reqs.length).toBe(1);
    reqs[0].flush(mockLoginResp);
    expect(results).toEqual(['at-test', 'at-test']);
  });

  it('should mark as unauthenticated via markAsUnauthenticated()', () => {
    tokenStorage.setAccessToken('token');
    service.markAsUnauthenticated();
    expect(service.sessionState()).toBe('NO_AUTENTICADA');
    expect(tokenStorage.getAccessToken()).toBeNull();
  });

  it('should clear state on logout error (best-effort)', () => {
    tokenStorage.setAccessToken('token');
    service.logout().subscribe({ error: () => {} });
    const req = httpMock.expectOne('/api/v1/auth/web/logout');
    req.flush({ message: 'Server Error' }, { status: 500, statusText: 'Server Error' });
    expect(service.sessionState()).toBe('NO_AUTENTICADA');
    expect(tokenStorage.getAccessToken()).toBeNull();
  });

  it('should reset refreshInFlight after successful refresh', () => {
    service.refresh().subscribe();
    httpMock.expectOne('/api/v1/auth/web/refresh').flush(mockLoginResp);

    // Segunda llamada debe generar una nueva solicitud (refreshInFlight reseteado)
    service.refresh().subscribe();
    httpMock.expectOne('/api/v1/auth/web/refresh').flush(mockLoginResp);

    expect(service.sessionState()).toBe('AUTENTICADA');
  });
});
