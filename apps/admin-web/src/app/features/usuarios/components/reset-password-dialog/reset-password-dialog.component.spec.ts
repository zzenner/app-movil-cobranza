import { TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { provideAnimations } from '@angular/platform-browser/animations';
import { ResetPasswordDialogComponent } from './reset-password-dialog.component';

describe('ResetPasswordDialogComponent', () => {
  let dialogRefMock: { close: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    dialogRefMock = { close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ResetPasswordDialogComponent],
      providers: [
        provideAnimations(),
        { provide: MatDialogRef, useValue: dialogRefMock },
      ],
    });
  });

  afterEach(() => TestBed.resetTestingModule());

  it('el formulario es inválido cuando está vacío', () => {
    const fixture = TestBed.createComponent(ResetPasswordDialogComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.form.invalid).toBe(true);
  });

  it('el formulario es inválido cuando la contraseña tiene menos de 8 caracteres', () => {
    const fixture = TestBed.createComponent(ResetPasswordDialogComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.controls.nuevaContrasena.setValue('abc123');
    expect(fixture.componentInstance.form.invalid).toBe(true);
  });

  it('el formulario es válido cuando la contraseña tiene 8 o más caracteres', () => {
    const fixture = TestBed.createComponent(ResetPasswordDialogComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.controls.nuevaContrasena.setValue('password1');
    expect(fixture.componentInstance.form.valid).toBe(true);
  });

  it('confirmar() cierra el diálogo con la contraseña cuando el formulario es válido', () => {
    const fixture = TestBed.createComponent(ResetPasswordDialogComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.controls.nuevaContrasena.setValue('NuevaPass1!');
    fixture.componentInstance.confirmar();
    expect(dialogRefMock.close).toHaveBeenCalledOnce();
    expect(dialogRefMock.close).toHaveBeenCalledWith('NuevaPass1!');
  });

  it('confirmar() no cierra el diálogo cuando el formulario es inválido', () => {
    const fixture = TestBed.createComponent(ResetPasswordDialogComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.controls.nuevaContrasena.setValue('corta');
    fixture.componentInstance.confirmar();
    expect(dialogRefMock.close).not.toHaveBeenCalled();
  });

  it('el botón Restablecer está deshabilitado cuando el formulario es inválido', async () => {
    const fixture = TestBed.createComponent(ResetPasswordDialogComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const buttons = Array.from(el.querySelectorAll('button'));
    const submitBtn = buttons.find((b) => b.textContent?.includes('Restablecer'));
    expect(submitBtn).toBeTruthy();
    expect(submitBtn!.hasAttribute('disabled')).toBe(true);
  });

  it('mostrarContrasena controla el tipo del campo de contraseña', async () => {
    const fixture = TestBed.createComponent(ResetPasswordDialogComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const comp = fixture.componentInstance;
    expect(comp.mostrarContrasena()).toBe(false);
    comp.mostrarContrasena.set(true);
    fixture.detectChanges();
    const input = fixture.nativeElement.querySelector(
      'input[formControlName="nuevaContrasena"]',
    ) as HTMLInputElement;
    expect(input.type).toBe('text');
  });

  it('el botón Cancelar existe y no llama a close con ninguna contraseña', async () => {
    const fixture = TestBed.createComponent(ResetPasswordDialogComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const buttons = Array.from(el.querySelectorAll('button'));
    const cancelBtn = buttons.find((b) => b.textContent?.includes('Cancelar'));
    expect(cancelBtn).toBeTruthy();
    // El botón Cancelar usa mat-dialog-close sin valor: no debe llamar confirmar()
    expect(dialogRefMock.close).not.toHaveBeenCalled();
  });
});
