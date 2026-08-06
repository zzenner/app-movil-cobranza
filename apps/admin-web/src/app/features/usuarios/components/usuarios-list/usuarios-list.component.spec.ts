import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { type Mocked } from 'vitest';
import { UsuariosListComponent } from './usuarios-list.component';
import { UsuariosService } from '../../services/usuarios.service';

const mockListado = {
  contenido: [
    {
      id: 'u1',
      nombreUsuario: 'admin.test',
      nombres: 'Admin',
      apellidoPaterno: 'Test',
      apellidoMaterno: null,
      correo: null,
      estadoCalculado: 'ACTIVO' as const,
      bloqueadoHasta: null,
      roles: ['JEFE_SUPERVISORES'],
      supervisorId: null,
      supervisorNombreUsuario: null,
      fechaCreacion: '2026-08-01T00:00:00Z',
    },
  ],
  pagina: 0,
  tamanio: 20,
  totalElementos: 1,
  totalPaginas: 1,
};

const mockVacio = { contenido: [], pagina: 0, tamanio: 20, totalElementos: 0, totalPaginas: 0 };

describe('UsuariosListComponent', () => {
  let serviceMock: Mocked<UsuariosService>;

  beforeEach(async () => {
    serviceMock = {
      listar: vi.fn().mockReturnValue(of(mockListado)),
      obtenerDetalle: vi.fn(),
    } as unknown as Mocked<UsuariosService>;

    await TestBed.configureTestingModule({
      imports: [UsuariosListComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimations(),
        { provide: UsuariosService, useValue: serviceMock },
      ],
    }).compileComponents();
  });

  it('carga usuarios al inicializar', async () => {
    const fixture = TestBed.createComponent(UsuariosListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(serviceMock.listar).toHaveBeenCalledWith(0, 20, expect.any(Object));
    expect(fixture.componentInstance.usuarios()).toHaveLength(1);
  });

  it('muestra estado vacío cuando no hay usuarios', async () => {
    serviceMock.listar.mockReturnValue(of(mockVacio));
    const fixture = TestBed.createComponent(UsuariosListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="empty-listado"]')).toBeTruthy();
  });

  it('muestra error cuando la API falla', async () => {
    serviceMock.listar.mockReturnValue(throwError(() => new Error('network')));
    const fixture = TestBed.createComponent(UsuariosListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.componentInstance.error()).toBe(true);
  });

  it('aplica debounce de 400ms al filtrar por nombre', async () => {
    vi.useFakeTimers();
    const fixture = TestBed.createComponent(UsuariosListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const callsBefore = (serviceMock.listar as ReturnType<typeof vi.fn>).mock.calls.length;
    fixture.componentInstance.filtrosForm.get('nombreUsuario')!.setValue('test');
    vi.advanceTimersByTime(300);
    expect((serviceMock.listar as ReturnType<typeof vi.fn>).mock.calls.length).toBe(callsBefore);
    vi.advanceTimersByTime(100);
    vi.useRealTimers();
  });
});
