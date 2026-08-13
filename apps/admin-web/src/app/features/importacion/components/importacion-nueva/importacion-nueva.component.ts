import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { ImportacionService } from '../../services/importacion.service';

@Component({
  selector: 'app-importacion-nueva',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatButtonModule, MatProgressSpinnerModule, MatIconModule,
  ],
  template: `
    <div class="page-header">
      <h1>Nueva importación mensual</h1>
      <a mat-button routerLink="/importacion">Volver</a>
    </div>

    <div class="form-contenedor">
      <p class="instruccion">
        Seleccione el archivo CSV de importación mensual.
        El período y la cartera se obtienen de cada fila del archivo.
      </p>

      <div class="archivo-selector">
        <input type="file" accept=".csv" (change)="onArchivoSeleccionado($event)"
               #archivoInput class="archivo-input" data-testid="input-archivo" />
        <button type="button" mat-stroked-button (click)="archivoInput.click()">
          <mat-icon>attach_file</mat-icon>
          {{ archivoSeleccionado() ? archivoSeleccionado()!.name : 'Seleccionar CSV' }}
        </button>
        @if (mostrarErrorArchivo()) {
          <span class="error-archivo" data-testid="error-archivo">Debe seleccionar un archivo CSV</span>
        }
      </div>

      @if (errorServidor()) {
        <div class="error-servidor" data-testid="error-servidor">
          <mat-icon color="warn">error</mat-icon>
          {{ errorServidor() }}
        </div>
      }

      <div class="acciones">
        <button mat-raised-button color="primary" type="button" (click)="enviar()"
                [disabled]="enviando()" data-testid="btn-subir">
          @if (enviando()) { <mat-spinner diameter="18" /> }
          @else { <mat-icon>cloud_upload</mat-icon> }
          Subir y validar
        </button>
      </div>
    </div>
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
    .form-contenedor { display: flex; flex-direction: column; gap: 16px; max-width: 500px; }
    .instruccion { color: #555; font-size: 14px; margin: 0; }
    .archivo-selector { display: flex; flex-direction: column; gap: 8px; }
    .archivo-input { display: none; }
    .error-archivo { color: #c62828; font-size: 12px; }
    .error-servidor { display: flex; align-items: center; gap: 8px; color: #c62828; padding: 8px; background: #ffebee; border-radius: 4px; }
    .acciones { display: flex; gap: 12px; padding-top: 8px; }
  `,
})
export class ImportacionNuevaComponent {
  private readonly service = inject(ImportacionService);
  private readonly router = inject(Router);

  readonly archivoSeleccionado = signal<File | null>(null);
  readonly mostrarErrorArchivo = signal(false);
  readonly enviando = signal(false);
  readonly errorServidor = signal<string | null>(null);

  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.archivoSeleccionado.set(file);
    this.mostrarErrorArchivo.set(false);
  }

  enviar(): void {
    const archivo = this.archivoSeleccionado();
    if (!archivo) { this.mostrarErrorArchivo.set(true); return; }
    if (this.enviando()) return;

    this.enviando.set(true);
    this.errorServidor.set(null);

    this.service.crear('LEGADO', archivo).subscribe({
      next: (r) => this.router.navigate(['/importacion', r.importacionId]),
      error: (err) => {
        this.enviando.set(false);
        const msg = err?.error?.detail ?? err?.error?.message ?? 'Error al subir el archivo';
        this.errorServidor.set(msg);
      },
    });
  }
}
