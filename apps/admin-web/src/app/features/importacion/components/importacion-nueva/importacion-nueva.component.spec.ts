import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { type Mocked } from 'vitest';
import { ImportacionNuevaComponent } from './importacion-nueva.component';
import { ImportacionService } from '../../services/importacion.service';

const mockRespuesta = {
  importacionId: 'imp-nuevo',
  estado: 'RECIBIDA',
  periodo: null,
  nombreArchivoOriginal: 'importacion.csv',
};

describe('ImportacionNuevaComponent', () => {
  let serviceMock: Mocked<ImportacionService>;

  beforeEach(async () => {
    serviceMock = {
      listarCarteras: vi.fn(),
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

  // ── Contrato v2: sin selector de cartera ni campo de período ──────────────

  it('el formulario NO muestra selector de cartera (contrato v2)', async () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="selector-cartera"]')).toBeNull();
  });

  it('el formulario NO muestra campo de período (contrato v2)', async () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="input-periodo"]')).toBeNull();
  });

  it('el componente NO llama a listarCarteras (cartera viene en el CSV)', () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    expect(serviceMock.listarCarteras).not.toHaveBeenCalled();
  });

  // ── Selector de archivo ───────────────────────────────────────────────────

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

  // ── Envío — validación de archivo ─────────────────────────────────────────

  it('enviar muestra error de archivo si no hay archivo seleccionado', () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    comp.enviar();
    expect(comp.mostrarErrorArchivo()).toBe(true);
    expect(serviceMock.crear).not.toHaveBeenCalled();
  });

  // ── Envío correcto — contrato v2 ──────────────────────────────────────────

  it('enviar llama crear con sistemaOrigen y archivo únicamente (sin carteraId, sin periodo)', async () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const comp = fixture.componentInstance;
    const file = new File(['rut;nombre'], 'importacion.csv', { type: 'text/csv' });
    comp.archivoSeleccionado.set(file);
    comp.enviar();
    expect(serviceMock.crear).toHaveBeenCalledWith('LEGADO', file);
    expect(serviceMock.crear).toHaveBeenCalledTimes(1);
  });

  it('enviar NO pasa carteraId como segundo argumento', async () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const comp = fixture.componentInstance;
    const file = new File(['x'], 'test.csv');
    comp.archivoSeleccionado.set(file);
    comp.enviar();
    const args = serviceMock.crear.mock.calls[0];
    // La firma es crear(sistemaOrigen, archivo) — solo 2 argumentos
    expect(args).toHaveLength(2);
    expect(args[0]).toBe('LEGADO');
    expect(args[1]).toBe(file);
  });

  it('la subida navega a detalle de importacion cuando es exitosa', async () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const comp = fixture.componentInstance;
    const file = new File(['rut;nombre'], 'importacion.csv', { type: 'text/csv' });
    comp.archivoSeleccionado.set(file);
    comp.enviar();
    expect(serviceMock.crear).toHaveBeenCalled();
  });

  // ── Manejo de errores del servidor ────────────────────────────────────────

  it('muestra error del servidor cuando crear falla con ARCHIVO_YA_IMPORTADO', async () => {
    const errorBody = { detail: 'ARCHIVO_YA_IMPORTADO: el archivo ya fue procesado' };
    serviceMock.crear.mockReturnValue(throwError(() => ({ error: errorBody })));
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const comp = fixture.componentInstance;
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
    comp.archivoSeleccionado.set(new File(['x'], 'f.csv'));
    comp.enviar();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="error-servidor"]')).toBeTruthy();
  });

  it('enviando vuelve a false cuando crear falla', async () => {
    serviceMock.crear.mockReturnValue(throwError(() => ({ error: { detail: 'error' } })));
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const comp = fixture.componentInstance;
    comp.archivoSeleccionado.set(new File(['x'], 'f.csv'));
    comp.enviar();
    expect(comp.enviando()).toBe(false);
  });

  it('el template muestra el selector de archivo CSV', async () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const inputArchivo = el.querySelector('[data-testid="input-archivo"]') as HTMLInputElement;
    expect(inputArchivo).toBeTruthy();
    expect(inputArchivo.accept).toContain('.csv');
  });

  it('el template muestra el botón de subir', async () => {
    const fixture = TestBed.createComponent(ImportacionNuevaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const btn = el.querySelector('[data-testid="btn-subir"]');
    expect(btn).toBeTruthy();
  });
});
