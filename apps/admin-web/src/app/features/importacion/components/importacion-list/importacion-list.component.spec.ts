import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { type Mocked } from 'vitest';
import { ImportacionListComponent } from './importacion-list.component';
import { ImportacionService } from '../../services/importacion.service';

const mockCartera = { id: 'cart-001', nombre: 'Cartera Test' };

const mockPagina = {
  contenido: [
    {
      id: 'imp-001',
      carteraId: 'cart-001',
      periodo: '2026-08',
      sistemaOrigen: 'LEGADO',
      estado: 'COMPLETADA' as const,
      nombreArchivoOriginal: 'archivo.csv',
      filasTotales: 5,
      filasProcesadas: 5,
      filasRechazadas: 0,
      filasAdvertencia: 0,
      fechaCreacion: '2026-08-09T00:00:00Z',
      fechaActualizacion: '2026-08-09T00:10:00Z',
    },
  ],
  pagina: 0,
  tamanio: 20,
  totalElementos: 1,
  totalPaginas: 1,
};

const mockVacio = { contenido: [], pagina: 0, tamanio: 20, totalElementos: 0, totalPaginas: 0 };

describe('ImportacionListComponent', () => {
  let serviceMock: Mocked<ImportacionService>;

  beforeEach(async () => {
    serviceMock = {
      listar: vi.fn().mockReturnValue(of(mockPagina)),
      listarCarteras: vi.fn().mockReturnValue(of([mockCartera])),
      obtener: vi.fn(),
      listarErrores: vi.fn(),
      confirmar: vi.fn(),
      crear: vi.fn(),
    } as unknown as Mocked<ImportacionService>;

    await TestBed.configureTestingModule({
      imports: [ImportacionListComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimations(),
        { provide: ImportacionService, useValue: serviceMock },
      ],
    }).compileComponents();
  });

  it('carga importaciones al inicializar', async () => {
    const fixture = TestBed.createComponent(ImportacionListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(serviceMock.listar).toHaveBeenCalledWith(0, 20, undefined);
    expect(fixture.componentInstance.importaciones()).toHaveLength(1);
  });

  it('carga carteras para el filtro al inicializar', async () => {
    const fixture = TestBed.createComponent(ImportacionListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(serviceMock.listarCarteras).toHaveBeenCalled();
    expect(fixture.componentInstance.carteras()).toContainEqual(mockCartera);
  });

  it('muestra estado vacío cuando no hay importaciones', async () => {
    serviceMock.listar.mockReturnValue(of(mockVacio));
    const fixture = TestBed.createComponent(ImportacionListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="empty-listado"]')).toBeTruthy();
  });

  it('muestra error cuando la API falla', async () => {
    serviceMock.listar.mockReturnValue(throwError(() => new Error('network')));
    const fixture = TestBed.createComponent(ImportacionListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="error-listado"]')).toBeTruthy();
  });

  it('muestra botón de nueva importación', async () => {
    const fixture = TestBed.createComponent(ImportacionListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="btn-nueva-importacion"]')).toBeTruthy();
  });

  it('totalElementos refleja el valor de la API', async () => {
    const fixture = TestBed.createComponent(ImportacionListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.componentInstance.totalElementos()).toBe(1);
  });

  it('estado COMPLETADA se muestra en la tabla', async () => {
    const fixture = TestBed.createComponent(ImportacionListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('COMPLETADA');
  });

  it('muestra período correcto en la tabla', async () => {
    const fixture = TestBed.createComponent(ImportacionListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('2026-08');
  });
});
