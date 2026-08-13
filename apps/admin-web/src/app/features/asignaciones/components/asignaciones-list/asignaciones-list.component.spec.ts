import { describe, it, expect, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AsignacionesListComponent } from './asignaciones-list.component';
import { AsignacionesService } from '../../services/asignaciones.service';
import { ItemAsignacionDiariaAdmin } from '../../models/asignacion.models';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

const mockAsignacion: ItemAsignacionDiariaAdmin = {
  id: 'a1', fecha: '2026-08-15', periodo: '2026-08',
  carteraId: 'c1', nombreCartera: 'Cartera A',
  ejecutivoId: 'e1', nombreEjecutivo: 'Ana López',
  supervisorId: 's1', nombreSupervisor: 'Juan Pérez',
  estado: 'BORRADOR', fechaPublicacion: null, cantidadPersonas: 5,
};

function mockService(data: ItemAsignacionDiariaAdmin[] = [mockAsignacion]) {
  return { listarDiarias: vi.fn(() => of(data)) };
}

describe('AsignacionesListComponent', () => {
  it('carga asignaciones al iniciar', () => {
    TestBed.configureTestingModule({
      imports: [AsignacionesListComponent],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AsignacionesService, useValue: mockService() },
      ],
    });

    const fixture = TestBed.createComponent(AsignacionesListComponent);
    fixture.detectChanges();

    const comp = fixture.componentInstance;
    expect(comp.asignaciones()).toHaveLength(1);
    expect(comp.cargando()).toBe(false);
  });

  it('muestra error si el servicio falla', () => {
    const svc = { listarDiarias: vi.fn(() => throwError(() => new Error('fail'))) };
    TestBed.configureTestingModule({
      imports: [AsignacionesListComponent],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AsignacionesService, useValue: svc },
      ],
    });

    const fixture = TestBed.createComponent(AsignacionesListComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toBeTruthy();
  });

  it('etiqueta traduce correctamente los estados', () => {
    TestBed.configureTestingModule({
      imports: [AsignacionesListComponent],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AsignacionesService, useValue: mockService() },
      ],
    });
    const comp = TestBed.createComponent(AsignacionesListComponent).componentInstance;
    expect(comp.etiqueta('BORRADOR')).toBe('Borrador');
    expect(comp.etiqueta('PUBLICADA')).toBe('Publicada');
    expect(comp.etiqueta('FINALIZADA')).toBe('Finalizada');
    expect(comp.etiqueta('CANCELADA')).toBe('Cancelada');
  });
});
