import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

export interface ConfirmActionDialogData {
  titulo: string;
  mensaje: string;
  accion: string;
  color?: 'warn' | 'primary';
}

@Component({
  selector: 'app-confirm-action-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.titulo }}</h2>
    <mat-dialog-content>{{ data.mensaje }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-button [color]="data.color ?? 'primary'" (click)="confirmar()">
        {{ data.accion }}
      </button>
    </mat-dialog-actions>
  `,
})
export class ConfirmActionDialogComponent {
  readonly dialogRef = inject(MatDialogRef<ConfirmActionDialogComponent>);
  readonly data = inject<ConfirmActionDialogData>(MAT_DIALOG_DATA);

  confirmar(): void {
    this.dialogRef.close(true);
  }
}
