import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { type Mocked } from 'vitest';
import { ImportacionNuevaComponent } from './importacion-nueva.component';
import { ImportacionService } from '../../services/importacion.service';

const mockCarteras = [
  { id: 'cart-001', nombre: 'Cartera Test' },
  { id: 'cart-002', nombre: 'Cartera B' },
];

const mockRespuesta = {
  importacionId: 'imp-nuevo',
  estado: 'RECIBIDA',
  periodo: '2026-08',
  nombreArchivoOriginal: 'importacion.csv',
};

describe('ImportacionNuevaComponent', () => {
  let serviceMock: Mocked<ImportacionService>;

  beforeEach(async () => {
    serviceMock = {
      listarCarteras: vi.fn().mockReturnValue(of(mockCarteras)),
      crear: vi.fn().mockReturnValue(of(mockRespuesta)),
      listar: vi.fn(),
      obtener: vi.fn(),
      listarErrores: vi.fn(),
      confirmar: vi.fn(),
    } as unknown as Mocked<ImportacionService>;

    await TestBed.configureTestingModule({
      imports: [ImportacionNuevaComponent],
      providers: [
        provideRouter([{ path: 'importacion/:id', component: ImportacionNuevaComponent }]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimations(),
        { provide: ImportacionService, useValue: serviceMock },
      ],
    }).compileComponents();
  });

  it('carga carteras al inicializar', async () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(serviceMock.listarCarteras).toHaveBeenCalled();
    expect(fixture.componentInstance.carteras()).toHaveLength(2);
    expect(fixture.componentInstance.cargandoCarteras()).toBe(false);
  });

  it('formulario es inválido si carteraId está vacío', () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    comp.form.setValue({ carteraId: '', periodo: '2026-08' });
    expect(comp.form.invalid).toBe(true);
  });

  it('formulario es inválido si período no cumple el patrón YYYY-MM', () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    comp.form.setValue({ carteraId: 'cart-001', periodo: '08-2026' });
    expect(comp.form.get('periodo')?.hasError('pattern')).toBe(true);
  });

  it('formulario es válido con carteraId y período correcto', () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    comp.form.setValue({ carteraId: 'cart-001', periodo: '2026-08' });
    expect(comp.form.valid).toBe(true);
  });

  it('periodo 2026-01 es válido', () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    comp.form.setValue({ carteraId: 'cart-001', periodo: '2026-01' });
    expect(comp.form.get('periodo')?.valid).toBe(true);
  });

  it('periodo 2026-12 es válido', () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    comp.form.setValue({ carteraId: 'cart-001', periodo: '2026-12' });
    expect(comp.form.get('periodo')?.valid).toBe(true);
  });

  it('periodo 2026-13 es inválido', () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    comp.form.setValue({ carteraId: 'cart-001', periodo: '2026-13' });
    expect(comp.form.get('periodo')?.hasError('pattern')).toBe(true);
  });

  it('onArchivoSeleccionado actualiza archivoSeleccionado', () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    const file = new File(['contenido'], 'datos.csv', { type: 'text/csv' });
    const mockEvent = { target: { files: [file] } } as unknown as Event;
    comp.onArchivoSeleccionado(mockEvent);
    expect(comp.archivoSeleccionado()).toBe(file);
    expect(comp.mostrarErrorArchivo()).toBe(false);
  });

  it('enviar muestra error de archivo si no hay archivo seleccionado', () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    comp.form.setValue({ carteraId: 'cart-001', periodo: '2026-08' });
    comp.enviar();
    expect(comp.mostrarErrorArchivo()).toBe(true);
    expect(serviceMock.crear).not.toHaveBeenCalled();
  });

  it('enviar no procede si el formulario es inválido', () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    comp.form.setValue({ carteraId: '', periodo: '' });
    comp.enviar();
    expect(serviceMock.crear).not.toHaveBeenCalled();
  });

  it('enviar llama crear con los parámetros correctos', async () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const comp = fixture.componentInstance;
    comp.form.setValue({ carteraId: 'cart-001', periodo: '2026-08' });
    const file = new File(['rut;nombre'], 'importacion.csv', { type: 'text/csv' });
    comp.archivoSeleccionado.set(file);
    comp.enviar();
    expect(serviceMock.crear).toHaveBeenCalledWith('cart-001', '2026-08', 'LEGADO', file);
  });

  it('muestra error del servidor cuando crear falla con ARCHIVO_YA_IMPORTADO', async () => {
    const errorBody = { detail: 'ARCHIVO_YA_IMPORTADO: el archivo ya fue procesado' };
    serviceMock.crear.mockReturnValue(throwError(() => ({ error: errorBody })));
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const comp = fixture.componentInstance;
    comp.form.setValue({ carteraId: 'cart-001', periodo: '2026-08' });
    const file = new File(['contenido'], 'importacion.csv');
    comp.archivoSeleccionado.set(file);
    comp.enviar();
    expect(comp.errorServidor()).toContain('ARCHIVO_YA_IMPORTADO');
    expect(comp.enviando()).toBe(false);
  });

  it('muestra error genérico cuando crear falla sin detail', async () => {
    serviceMock.crear.mockReturnValue(throwError(() => ({ error: {} })));
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const comp = fixture.componentInstance;
    comp.form.setValue({ carteraId: 'cart-001', periodo: '2026-08' });
    comp.archivoSeleccionado.set(new File(['x'], 'f.csv'));
    comp.enviar();
    expect(comp.errorServidor()).toBeTruthy();
  });

  it('muestra el área de error del servidor en el template', async () => {
    const errorBody = { detail: 'PERIODO_ANTERIOR_NO_PERMITIDO' };
    serviceMock.crear.mockReturnValue(throwError(() => ({ error: errorBody })));
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    comp.form.setValue({ carteraId: 'cart-001', periodo: '2025-01' });
    comp.archivoSeleccionado.set(new File(['x'], 'f.csv'));
    comp.enviar();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="error-servidor"]')).toBeTruthy();
  });

  it('cargandoCarteras empieza true y termina false después de carga', async () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    expect(fixture.componentInstance.cargandoCarteras()).toBe(true);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.componentInstance.cargandoCarteras()).toBe(false);
  });

  it('cargandoCarteras termina false aunque falle la carga de carteras', async () => {
    serviceMock.listarCarteras.mockReturnValue(throwError(() => new Error('network')));
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.componentInstance.cargandoCarteras()).toBe(false);
  });
});
