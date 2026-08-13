import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ItemEjecutivoAdmin } from '../../models/supervision.models';

export interface ActualizarCodigoDialogData {
  ejecutivo: ItemEjecutivoAdmin;
}

@Component({
  selector: 'app-actualizar-codigo-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>Actualizar código de ejecutivo</h2>
    <mat-dialog-content>
      <p>Ejecutivo: <strong>{{ data.ejecutivo.nombreCompleto }}</strong></p>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Código externo</mat-label>
        <input matInput [formControl]="codigoCtrl" maxlength="50" placeholder="Ej: EJ-0042" />
        <mat-hint>Máximo 50 caracteres. Vacío para eliminar.</mat-hint>
        @if (codigoCtrl.hasError('maxlength')) {
          <mat-error>Máximo 50 caracteres</mat-error>
        }
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-raised-button color="primary"
              [disabled]="codigoCtrl.invalid"
              (click)="confirmar()">
        Guardar
      </button>
    </mat-dialog-actions>
  `,
  styles: `.full-width { width: 100%; }`,
})
export class ActualizarCodigoDialogComponent {
  readonly data: ActualizarCodigoDialogData = inject(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ActualizarCodigoDialogComponent>);

  readonly codigoCtrl = new FormControl(
    this.data.ejecutivo.codigoEjecutivoOrigen ?? '',
    Validators.maxLength(50),
  );

  confirmar(): void {
    if (this.codigoCtrl.valid) {
      const val = this.codigoCtrl.value;
      this.dialogRef.close(val?.trim() || null);
    }
  }
}
