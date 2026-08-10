import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { ImportacionService } from '../../services/importacion.service';
import { ItemCartera } from '../../models/importacion.models';

@Component({
  selector: 'app-importacion-nueva',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatProgressSpinnerModule, MatIconModule,
  ],
  template: `
    <div class="page-header">
      <h1>Nueva importación mensual</h1>
      <a mat-button routerLink="/importacion">Volver</a>
    </div>

    @if (cargandoCarteras()) {
      <div class="loading-container"><mat-spinner diameter="32" /></div>
    } @else {
      <form [formGroup]="form" (ngSubmit)="enviar()" class="form-contenedor">

        <mat-form-field appearance="outline" class="campo-completo">
          <mat-label>Cartera</mat-label>
          <mat-select formControlName="carteraId" data-testid="selector-cartera">
            @for (c of carteras(); track c.id) {
              <mat-option [value]="c.id">{{ c.nombre }}</mat-option>
            }
          </mat-select>
          @if (form.get('carteraId')?.hasError('required') && form.get('carteraId')?.touched) {
            <mat-error>Seleccione una cartera</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="campo-completo">
          <mat-label>Período (YYYY-MM)</mat-label>
          <input matInput formControlName="periodo" placeholder="2026-08" data-testid="input-periodo" />
          @if (form.get('periodo')?.hasError('required') && form.get('periodo')?.touched) {
            <mat-error>El período es requerido</mat-error>
          }
          @if (form.get('periodo')?.hasError('pattern') && form.get('periodo')?.touched) {
            <mat-error>Formato inválido. Use YYYY-MM (ej: 2026-08)</mat-error>
          }
        </mat-form-field>

        <div class="archivo-selector">
          <input type="file" accept=".csv" (change)="onArchivoSeleccionado($event)"
                 #archivoInput class="archivo-input" data-testid="input-archivo" />
          <button type="button" mat-stroked-button (click)="archivoInput.click()">
            <mat-icon>attach_file</mat-icon>
            {{ archivoSeleccionado() ? archivoSeleccionado()!.name : 'Seleccionar CSV' }}
          </button>
          @if (mostrarErrorArchivo()) {
            <span class="error-archivo">Debe seleccionar un archivo CSV</span>
          }
        </div>

        @if (errorServidor()) {
          <div class="error-servidor" data-testid="error-servidor">
            <mat-icon color="warn">error</mat-icon>
            {{ errorServidor() }}
          </div>
        }

        <div class="acciones">
          <button mat-raised-button color="primary" type="submit"
                  [disabled]="enviando()" data-testid="btn-subir">
            @if (enviando()) { <mat-spinner diameter="18" /> }
            @else { <mat-icon>cloud_upload</mat-icon> }
            Subir y validar
          </button>
        </div>
      </form>
    }
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
    .form-contenedor { display: flex; flex-direction: column; gap: 16px; max-width: 500px; }
    .campo-completo { width: 100%; }
    .archivo-selector { display: flex; flex-direction: column; gap: 8px; }
    .archivo-input { display: none; }
    .error-archivo { color: #c62828; font-size: 12px; }
    .error-servidor { display: flex; align-items: center; gap: 8px; color: #c62828; padding: 8px; background: #ffebee; border-radius: 4px; }
    .acciones { display: flex; gap: 12px; padding-top: 8px; }
    .loading-container { display: flex; justify-content: center; padding: 48px; }
  `,
})
export class ImportacionNuevaComponent implements OnInit {
  private readonly service = inject(ImportacionService);
  private readonly router = inject(Router);

  readonly carteras = signal<ItemCartera[]>([]);
  readonly cargandoCarteras = signal(true);
  readonly archivoSeleccionado = signal<File | null>(null);
  readonly mostrarErrorArchivo = signal(false);
  readonly enviando = signal(false);
  readonly errorServidor = signal<string | null>(null);

  readonly form = new FormGroup({
    carteraId: new FormControl('', [Validators.required]),
    periodo: new FormControl('', [
      Validators.required,
      Validators.pattern(/^\d{4}-(0[1-9]|1[0-2])$/),
    ]),
  });

  ngOnInit(): void {
    this.service.listarCarteras().subscribe({
      next: (c) => { this.carteras.set(c); this.cargandoCarteras.set(false); },
      error: () => this.cargandoCarteras.set(false),
    });
  }

  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.archivoSeleccionado.set(file);
    this.mostrarErrorArchivo.set(false);
  }

  enviar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const archivo = this.archivoSeleccionado();
    if (!archivo) { this.mostrarErrorArchivo.set(true); return; }
    if (this.enviando()) return;

    this.enviando.set(true);
    this.errorServidor.set(null);

    const { carteraId, periodo } = this.form.value;
    this.service.crear(carteraId!, periodo!, 'LEGADO', archivo).subscribe({
      next: (r) => this.router.navigate(['/importacion', r.importacionId]),
      error: (err) => {
        this.enviando.set(false);
        const msg = err?.error?.detail ?? err?.error?.message ?? 'Error al subir el archivo';
        this.errorServidor.set(msg);
      },
    });
  }
}
