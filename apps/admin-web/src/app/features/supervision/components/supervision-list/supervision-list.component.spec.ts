import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { signal } from '@angular/core';
import { SupervisionListComponent } from './supervision-list.component';
import { SupervisionService } from '../../services/supervision.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { MatDialog } from '@angular/material/dialog';

const mockEjec = {
  usuarioId: 'ejec-1',
  nombreUsuario: 'ejec.uno',
  nombreCompleto: 'Ejec Uno',
  activo: true,
  codigoEjecutivoOrigen: null,
  supervisorId: null,
  supervisorNombre: null,
};

describe('SupervisionListComponent', () => {
  let fixture: ComponentFixture<SupervisionListComponent>;
  const serviceMock = {
    listarEjecutivos: vi.fn().mockReturnValue(of([mockEjec])),
    listarSupervisores: vi.fn().mockReturnValue(of([])),
    quitarSupervision: vi.fn().mockReturnValue(of(undefined)),
  };
  const authMock = { profile: signal(null) };

  beforeEach(async () => {
    serviceMock.listarEjecutivos = vi.fn().mockReturnValue(of([mockEjec]));
    serviceMock.listarSupervisores = vi.fn().mockReturnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [SupervisionListComponent],
      providers: [
        provideNoopAnimations(),
        { provide: SupervisionService, useValue: serviceMock },
        { provide: AuthService, useValue: authMock },
        { provide: MatDialog, useValue: { open: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SupervisionListComponent);
  });

  it('carga ejecutivos al iniciar', () => {
    fixture.detectChanges();
    expect(serviceMock.listarEjecutivos).toHaveBeenCalled();
    expect(fixture.componentInstance.ejecutivos()).toHaveLength(1);
    expect(fixture.componentInstance.cargando()).toBe(false);
  });

  it('muestra error cuando falla la carga', () => {
    serviceMock.listarEjecutivos.mockReturnValue(throwError(() => new Error()));
    fixture.detectChanges();
    expect(fixture.componentInstance.error()).not.toBeNull();
  });

  it('puedeAdministrar es false sin permisos', () => {
    fixture.detectChanges();
    expect(fixture.componentInstance.puedeAdministrar()).toBe(false);
  });
});
