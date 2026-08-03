import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { TokenStorageService } from '../auth/token-storage.service';
import { AuthService } from '../auth/auth.service';

const mockLoginResp = {
  accessToken: 'new-at',
  expiresInSeconds: 900,
  sessionExpiresAt: '2026-11-01T00:00:00Z',
};

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let tokenStorage: TokenStorageService;
  let authService: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    tokenStorage = TestBed.inject(TokenStorageService);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => httpMock.verify());

  it('añade header Authorization cuando hay token', () => {
    tokenStorage.setAccessToken('my-token');
    http.get('/api/v1/some-resource').subscribe();

    const req = httpMock.expectOne('/api/v1/some-resource');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-token');
    req.flush({});
  });

  it('no añade Authorization si no hay token', () => {
    http.get('/api/v1/some-resource').subscribe();

    const req = httpMock.expectOne('/api/v1/some-resource');
    expect(req.request.headers.get('Authorization')).toBeNull();
    req.flush({});
  });

  it('no intercepta rutas de autenticación', () => {
    tokenStorage.setAccessToken('my-token');
    http.post('/api/v1/auth/web/login', {}).subscribe();

    const req = httpMock.expectOne('/api/v1/auth/web/login');
    expect(req.request.headers.get('Authorization')).toBeNull();
    req.flush({});
  });

  it('no intercepta ruta de refresh', () => {
    tokenStorage.setAccessToken('my-token');
    http.post('/api/v1/auth/web/refresh', null).subscribe();

    const req = httpMock.expectOne('/api/v1/auth/web/refresh');
    expect(req.request.headers.get('Authorization')).toBeNull();
    req.flush({});
  });

  it('en 401 con token activo: reintenta con token renovado', () => {
    tokenStorage.setAccessToken('old-at');
    const results: unknown[] = [];

    http.get('/api/v1/protected').subscribe((r) => results.push(r));

    // Primera solicitud → 401
    const firstReq = httpMock.expectOne('/api/v1/protected');
    firstReq.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    // El interceptor llama a refresh
    httpMock.expectOne('/api/v1/auth/web/refresh').flush(mockLoginResp);

    // Reintento con nuevo token
    const retryReq = httpMock.expectOne('/api/v1/protected');
    expect(retryReq.request.headers.get('Authorization')).toBe('Bearer new-at');
    retryReq.flush({ data: 'ok' });

    expect(results).toHaveLength(1);
  });

  it('no reintenta si no había token (protección contra bucle)', () => {
    // Sin token — 401 no dispara refresh
    const errors: unknown[] = [];
    http.get('/api/v1/protected').subscribe({ error: (e) => errors.push(e) });

    httpMock
      .expectOne('/api/v1/protected')
      .flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    // No debe haber solicitud de refresh
    httpMock.expectNone('/api/v1/auth/web/refresh');
    expect(errors).toHaveLength(1);
  });
});
