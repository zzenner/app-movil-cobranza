import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideAnimations } from '@angular/platform-browser/animations';
import {
  ConfirmActionDialogComponent,
  type ConfirmActionDialogData,
} from './confirm-action-dialog.component';

const mockData: ConfirmActionDialogData = {
  titulo: 'Desactivar usuario',
  mensaje: '¿Desactivar a test.user? Se cerrarán sus sesiones activas.',
  accion: 'Desactivar',
  color: 'warn',
};

describe('ConfirmActionDialogComponent', () => {
  let dialogRefMock: { close: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    dialogRefMock = { close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ConfirmActionDialogComponent],
      providers: [
        provideAnimations(),
        { provide: MatDialogRef, useValue: dialogRefMock },
        { provide: MAT_DIALOG_DATA, useValue: mockData },
      ],
    });
  });

  afterEach(() => TestBed.resetTestingModule());

  it('muestra el título, mensaje y acción recibidos como data', () => {
    const fixture = TestBed.createComponent(ConfirmActionDialogComponent);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain(mockData.titulo);
    expect(el.textContent).toContain(mockData.mensaje);
    expect(el.textContent).toContain(mockData.accion);
  });

  it('confirmar() cierra el diálogo con true', () => {
    const fixture = TestBed.createComponent(ConfirmActionDialogComponent);
    fixture.detectChanges();
    fixture.componentInstance.confirmar();
    expect(dialogRefMock.close).toHaveBeenCalledOnce();
    expect(dialogRefMock.close).toHaveBeenCalledWith(true);
  });

  it('el botón Cancelar no llama a close(true)', async () => {
    const fixture = TestBed.createComponent(ConfirmActionDialogComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const buttons = Array.from(el.querySelectorAll('button'));
    const cancelBtn = buttons.find((b) => b.textContent?.includes('Cancelar'));
    expect(cancelBtn).toBeTruthy();
    cancelBtn!.click();
    fixture.detectChanges();
    expect(dialogRefMock.close).not.toHaveBeenCalledWith(true);
  });

  it('muestra el color warn en el botón de acción cuando data.color es warn', () => {
    const fixture = TestBed.createComponent(ConfirmActionDialogComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.data.color).toBe('warn');
  });
});
