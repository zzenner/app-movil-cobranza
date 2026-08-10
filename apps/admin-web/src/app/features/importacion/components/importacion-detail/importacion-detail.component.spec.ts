import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { type Mocked } from 'vitest';
import { ImportacionDetailComponent } from './importacion-detail.component';
import { ImportacionService } from '../../services/importacion.service';
import { EstadoImportacion, ImportacionDetalle } from '../../models/importacion.models';

const makeImportacion = (estado: EstadoImportacion, extra: Partial<Record<string, unknown>> = {}): ImportacionDetalle => ({
  id: 'imp-001',
  carteraId: 'cart-001',
  periodo: '2026-08',
  sistemaOrigen: 'LEGADO',
  estado,
  nombreArchivoOriginal: 'importacion.csv',
  filasTotales: 5,
  filasProcesadas: null,
  filasRechazadas: 0,
  filasAdvertencia: 0,
  usuarioId: 'usr-001',
  personasCreadas: null,
  personasActualizadas: null,
  operacionesCreadas: null,
  operacionesActualizadas: null,
  cuotasCreadas: null,
  cuotasActualizadas: null,
  mensajeError: null,
  fechaCreacion: '2026-08-09T00:00:00Z',
  fechaActualizacion: '2026-08-09T00:01:00Z',
  version: 2,
  ...extra,
});

const mockErroresPagina = {
  contenido: [
    {
      id: 'err-001',
      numeroFila: 2,
      columna: 'RUT_NUMERO',
      codigoError: 'RUT_INVALIDO_MODULO_11',
      nivel: 'ERROR' as const,
      mensaje: 'RUT inválido',
    },
  ],
  pagina: 0,
  tamanio: 50,
  totalElementos: 1,
  totalPaginas: 1,
};

const mockErroresVacio = { contenido: [], pagina: 0, tamanio: 50, totalElementos: 0, totalPaginas: 0 };

function createFixture(service: Mocked<ImportacionService>) {
  return TestBed.createComponent(ImportacionDetailComponent);
}

describe('ImportacionDetailComponent', () => {
  let serviceMock: Mocked<ImportacionService>;

  beforeEach(async () => {
    serviceMock = {
      obtener: vi.fn().mockReturnValue(of(makeImportacion('VALIDADA'))),
      listarErrores: vi.fn().mockReturnValue(of(mockErroresVacio)),
      confirmar: vi.fn().mockReturnValue(of(undefined)),
      listar: vi.fn(),
      listarCarteras: vi.fn(),
      crear: vi.fn(),
    } as unknown as Mocked<ImportacionService>;

    await TestBed.configureTestingModule({
      imports: [ImportacionDetailComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimations(),
        { provide: ImportacionService, useValue: serviceMock },
        {
          provide: ActivatedRoute,
          useValue: {
            params: of({ id: 'imp-001' }),
            snapshot: { params: { id: 'imp-001' } },
          },
        },
      ],
    }).compileComponents();
  });

  it('carga el detalle al inicializar', async () => {
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(serviceMock.obtener).toHaveBeenCalledWith('imp-001');
    expect(fixture.componentInstance.importacion()?.estado).toBe('VALIDADA');
  });

  it('muestra estado VALIDADA con badge correcto', async () => {
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const badge = el.querySelector('[data-testid="estado-badge"]');
    expect(badge?.textContent?.trim()).toContain('VALIDADA');
  });

  it('muestra panel confirmar cuando estado es VALIDADA', async () => {
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="panel-confirmar"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="btn-confirmar"]')).toBeTruthy();
  });

  it('NO muestra panel confirmar cuando estado es COMPLETADA', async () => {
    serviceMock.obtener.mockReturnValue(of(makeImportacion('COMPLETADA',
      { personasCreadas: 2, personasActualizadas: 0, operacionesCreadas: 2,
        operacionesActualizadas: 0, cuotasCreadas: 5, cuotasActualizadas: 0 })));
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="panel-confirmar"]')).toBeNull();
    expect(el.querySelector('[data-testid="panel-completada"]')).toBeTruthy();
  });

  it('muestra spinner de progreso cuando estado es RECIBIDA', async () => {
    serviceMock.obtener.mockReturnValue(of(makeImportacion('RECIBIDA')));
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="spinner-progreso"]')).toBeTruthy();
  });

  it('muestra spinner de progreso cuando estado es VALIDANDO', async () => {
    serviceMock.obtener.mockReturnValue(of(makeImportacion('VALIDANDO')));
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="spinner-progreso"]')).toBeTruthy();
  });

  it('muestra spinner de progreso cuando estado es PROCESANDO', async () => {
    serviceMock.obtener.mockReturnValue(of(makeImportacion('PROCESANDO')));
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="spinner-progreso"]')).toBeTruthy();
  });

  it('NO muestra spinner cuando estado es COMPLETADA', async () => {
    serviceMock.obtener.mockReturnValue(of(makeImportacion('COMPLETADA')));
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="spinner-progreso"]')).toBeNull();
  });

  it('muestra panel FALLIDA cuando estado es FALLIDA', async () => {
    serviceMock.obtener.mockReturnValue(of(makeImportacion('FALLIDA', { mensajeError: 'Error interno' })));
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="panel-fallida"]')).toBeTruthy();
  });

  it('muestra panel EXPIRADA cuando estado es EXPIRADA', async () => {
    serviceMock.obtener.mockReturnValue(of(makeImportacion('EXPIRADA')));
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="panel-expirada"]')).toBeTruthy();
  });

  it('carga errores cuando estado es CON_ERRORES', async () => {
    serviceMock.obtener.mockReturnValue(of(makeImportacion('CON_ERRORES', { filasRechazadas: 1 })));
    serviceMock.listarErrores.mockReturnValue(of(mockErroresPagina));
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(serviceMock.listarErrores).toHaveBeenCalledWith('imp-001', 0, 50);
  });

  it('tabla de errores se muestra cuando hay errores en CON_ERRORES', async () => {
    serviceMock.obtener.mockReturnValue(of(makeImportacion('CON_ERRORES', { filasRechazadas: 1 })));
    serviceMock.listarErrores.mockReturnValue(of(mockErroresPagina));
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="tabla-errores"]')).toBeTruthy();
  });

  it('muestra sin-errores cuando no hay errores en VALIDADA', async () => {
    serviceMock.listarErrores.mockReturnValue(of(mockErroresVacio));
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="sin-errores"]')).toBeTruthy();
  });

  it('muestra error de carga cuando la API falla', async () => {
    serviceMock.obtener.mockReturnValue(throwError(() => new Error('network')));
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.componentInstance.error()).toBe(true);
  });

  it('confirmar llama al servicio y recarga el detalle', async () => {
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    const comp = fixture.componentInstance;
    comp.confirmar();
    expect(serviceMock.confirmar).toHaveBeenCalledWith('imp-001');
  });

  it('confirmar establece errorConfirmar cuando falla', async () => {
    serviceMock.confirmar.mockReturnValue(throwError(() => ({ error: { detail: 'ESTADO_INVALIDO_PARA_CONFIRMAR' } })));
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    const comp = fixture.componentInstance;
    comp.confirmar();
    expect(comp.errorConfirmar()).toContain('ESTADO_INVALIDO_PARA_CONFIRMAR');
    expect(comp.confirmando()).toBe(false);
  });

  it('esEnProgreso devuelve true para RECIBIDA, VALIDANDO, PROCESANDO', () => {
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    expect(comp.esEnProgreso('RECIBIDA')).toBe(true);
    expect(comp.esEnProgreso('VALIDANDO')).toBe(true);
    expect(comp.esEnProgreso('PROCESANDO')).toBe(true);
  });

  it('esEnProgreso devuelve false para VALIDADA, COMPLETADA, CON_ERRORES, FALLIDA, EXPIRADA', () => {
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    expect(comp.esEnProgreso('VALIDADA')).toBe(false);
    expect(comp.esEnProgreso('COMPLETADA')).toBe(false);
    expect(comp.esEnProgreso('CON_ERRORES')).toBe(false);
    expect(comp.esEnProgreso('FALLIDA')).toBe(false);
    expect(comp.esEnProgreso('EXPIRADA')).toBe(false);
  });

  it('mensajeEstado devuelve texto descriptivo para estados en progreso', () => {
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    expect(comp.mensajeEstado('RECIBIDA')).toBeTruthy();
    expect(comp.mensajeEstado('VALIDANDO')).toBeTruthy();
    expect(comp.mensajeEstado('PROCESANDO')).toBeTruthy();
  });

  it('gestionarPolling inicia pollingSub para estado RECIBIDA', async () => {
    serviceMock.obtener.mockReturnValue(of(makeImportacion('RECIBIDA')));
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    // El polling se inicia en estados EN_PROGRESO
    expect(fixture.componentInstance.esEnProgreso('RECIBIDA')).toBe(true);
    // Verificamos internamente que la suscripción existe (private via cast)
    expect((fixture.componentInstance as any).pollingSub).toBeDefined();
    fixture.destroy();
  });

  it('NO inicia polling cuando estado inicial es COMPLETADA', async () => {
    serviceMock.obtener.mockReturnValue(of(makeImportacion('COMPLETADA')));
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    expect((fixture.componentInstance as any).pollingSub).toBeUndefined();
  });

  it('muestra panel de resultado cuando COMPLETADA tiene contadores', async () => {
    serviceMock.obtener.mockReturnValue(of(makeImportacion('COMPLETADA', {
      personasCreadas: 2, personasActualizadas: 1, operacionesCreadas: 2,
      operacionesActualizadas: 0, cuotasCreadas: 5, cuotasActualizadas: 2,
    })));
    const fixture = createFixture(serviceMock);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="panel-completada"]')).toBeTruthy();
    expect(el.textContent).toContain('Personas creadas');
  });
});
