import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { ItemEjecutivoAdmin, ItemSupervisorAdmin } from '../../models/supervision.models';

export interface AsignarDialogData {
  ejecutivo: ItemEjecutivoAdmin;
  supervisores: ItemSupervisorAdmin[];
}

@Component({
  selector: 'app-asignar-supervisor-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>
      {{ data.ejecutivo.supervisorId ? 'Reasignar supervisor' : 'Asignar supervisor' }}
    </h2>
    <mat-dialog-content>
      <p>Ejecutivo: <strong>{{ data.ejecutivo.nombreCompleto }}</strong></p>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Supervisor</mat-label>
        <mat-select [formControl]="supervisorCtrl">
          @for (s of data.supervisores; track s.usuarioId) {
            <mat-option [value]="s.usuarioId">{{ s.nombreCompleto }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-raised-button color="primary"
              [disabled]="supervisorCtrl.invalid"
              (click)="confirmar()">
        Confirmar
      </button>
    </mat-dialog-actions>
  `,
  styles: `.full-width { width: 100%; }`,
})
export class AsignarSupervisorDialogComponent {
  readonly data: AsignarDialogData = inject(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<AsignarSupervisorDialogComponent>);

  readonly supervisorCtrl = new FormControl(
    this.data.ejecutivo.supervisorId ?? '',
    Validators.required,
  );

  confirmar(): void {
    if (this.supervisorCtrl.valid) {
      this.dialogRef.close(this.supervisorCtrl.value);
    }
  }
}
