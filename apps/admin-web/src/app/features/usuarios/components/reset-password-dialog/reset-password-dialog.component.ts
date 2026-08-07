import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-reset-password-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
  ],
  template: `
    <h2 mat-dialog-title>Restablecer contraseña</h2>
    <mat-dialog-content>
      <form [formGroup]="form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Nueva contraseña</mat-label>
          <input matInput
                 [type]="mostrarContrasena() ? 'text' : 'password'"
                 formControlName="nuevaContrasena"
                 autocomplete="new-password" />
          <button mat-icon-button matSuffix type="button"
                  (click)="mostrarContrasena.set(!mostrarContrasena())">
            <mat-icon>{{ mostrarContrasena() ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
          @if (form.get('nuevaContrasena')?.hasError('required')) {
            <mat-error>La contraseña es obligatoria</mat-error>
          } @else if (form.get('nuevaContrasena')?.hasError('minlength')) {
            <mat-error>Mínimo 8 caracteres</mat-error>
          }
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-button color="warn" [disabled]="form.invalid" (click)="confirmar()">
        Restablecer
      </button>
    </mat-dialog-actions>
  `,
  styles: `.full-width { width: 100%; min-width: 320px; }`,
})
export class ResetPasswordDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly dialogRef = inject(MatDialogRef<ResetPasswordDialogComponent>);

  readonly mostrarContrasena = signal(false);

  readonly form = this.fb.nonNullable.group({
    nuevaContrasena: ['', [Validators.required, Validators.minLength(8)]],
  });

  confirmar(): void {
    if (this.form.valid) {
      this.dialogRef.close(this.form.getRawValue().nuevaContrasena);
    }
  }
}
