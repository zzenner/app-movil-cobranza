import { describe, it, expect, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AsignacionDetailComponent } from './asignacion-detail.component';
import { AsignacionesService } from '../../services/asignaciones.service';
import { DetalleAsignacionDiariaAdmin } from '../../models/asignacion.models';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

const mockDetalle: DetalleAsignacionDiariaAdmin = {
  id: 'd1', fecha: '2026-08-15', periodo: '2026-08',
  carteraId: 'c1', nombreCartera: 'Cartera A',
  ejecutivoId: 'e1', nombreEjecutivo: 'Ana López',
  supervisorId: 's1', nombreSupervisor: 'Juan Pérez',
  estado: 'BORRADOR',
  fechaPublicacion: null, publicadoPorId: null, nombrePublicador: null,
  motivoCancelacion: null, fechaCreacion: '2026-08-15T10:00:00Z', version: 0,
  cantidadPersonas: 2,
  personas: [
    { personaId: 'p1', rutNumero: '12345678', rutDv: '9', nombre: 'Carlos Fuentes' },
  ],
};

function makeService(detalle = mockDetalle) {
  return {
    obtenerDetalle: vi.fn(() => of(detalle)),
    publicar: vi.fn(() => of(undefined)),
    cancelar: vi.fn(() => of(undefined)),
  };
}

describe('AsignacionDetailComponent', () => {
  function setup(svc = makeService()) {
    TestBed.configureTestingModule({
      imports: [AsignacionDetailComponent],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AsignacionesService, useValue: svc },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'd1' } } } },
      ],
    });
    return TestBed.createComponent(AsignacionDetailComponent);
  }

  it('carga detalle al iniciar', () => {
    const fixture = setup();
    fixture.detectChanges();
    expect(fixture.componentInstance.detalle()?.id).toBe('d1');
    expect(fixture.componentInstance.cargando()).toBe(false);
  });

  it('muestra error si falla la carga', () => {
    const svc = { ...makeService(), obtenerDetalle: vi.fn(() => throwError(() => new Error())) };
    const fixture = setup(svc as any);
    fixture.detectChanges();
    expect(fixture.componentInstance.error()).toBeTruthy();
  });

  it('etiqueta devuelve texto legible', () => {
    const fixture = setup();
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    expect(comp.etiqueta('PUBLICADA')).toBe('Publicada');
    expect(comp.etiqueta('CANCELADA')).toBe('Cancelada');
  });
});
