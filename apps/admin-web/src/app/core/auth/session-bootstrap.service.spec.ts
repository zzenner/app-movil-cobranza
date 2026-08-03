import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { SessionBootstrapService } from './session-bootstrap.service';
import { AuthService } from './auth.service';

const mockLoginResp = {
  accessToken: 'at-bootstrap',
  expiresInSeconds: 900,
  sessionExpiresAt: '2026-11-01T00:00:00Z',
};

const mockProfile = {
  usuarioId: 'uuid-1',
  sesionId: 'uuid-2',
  dispositivoId: null,
  tipoCliente: 'WEB',
  nombreUsuario: 'admin',
  roles: ['ADMIN'],
  permisos: [],
};

describe('SessionBootstrapService', () => {
  let service: SessionBootstrapService;
  let authService: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SessionBootstrapService);
    authService = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('bootstrap con cookie válida: AUTENTICADA y perfil cargado', () => {
    let done = false;
    service.bootstrap().subscribe({ complete: () => (done = true) });

    httpMock.expectOne('/api/v1/auth/web/refresh').flush(mockLoginResp);
    httpMock.expectOne('/api/v1/auth/me').flush(mockProfile);

    expect(authService.sessionState()).toBe('AUTENTICADA');
    expect(authService.profile()?.nombreUsuario).toBe('admin');
    expect(done).toBe(true);
  });

  it('bootstrap sin cookie: NO_AUTENTICADA y perfil nulo', () => {
    let done = false;
    service.bootstrap().subscribe({ complete: () => (done = true) });

    httpMock
      .expectOne('/api/v1/auth/web/refresh')
      .flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(authService.sessionState()).toBe('NO_AUTENTICADA');
    expect(authService.profile()).toBeNull();
    expect(done).toBe(true);
  });

  it('bootstrap no propaga error — siempre completa', () => {
    let errored = false;
    let completed = false;
    service.bootstrap().subscribe({
      error: () => (errored = true),
      complete: () => (completed = true),
    });

    httpMock
      .expectOne('/api/v1/auth/web/refresh')
      .flush(null, { status: 500, statusText: 'Server Error' });

    expect(errored).toBe(false);
    expect(completed).toBe(true);
  });
});
