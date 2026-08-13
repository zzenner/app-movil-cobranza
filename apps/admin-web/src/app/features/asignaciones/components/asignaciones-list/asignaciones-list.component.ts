import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { AsignacionesService } from '../../services/asignaciones.service';
import { EstadoAsignacionDiaria, ItemAsignacionDiariaAdmin } from '../../models/asignacion.models';

@Component({
  selector: 'app-asignaciones-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatChipsModule,
  ],
  template: `
    <div class="page-header">
      <h1>Asignaciones Diarias</h1>
      <button mat-raised-button color="primary" routerLink="crear" data-testid="btn-nueva-asignacion">
        <mat-icon>add</mat-icon> Nueva asignación
      </button>
    </div>

    <div class="filtros">
      <mat-form-field appearance="outline">
        <mat-label>Fecha</mat-label>
        <input matInput type="date" [formControl]="fechaCtrl" />
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Estado</mat-label>
        <mat-select [formControl]="estadoCtrl">
          <mat-option value="">Todos</mat-option>
          <mat-option value="BORRADOR">Borrador</mat-option>
          <mat-option value="PUBLICADA">Publicada</mat-option>
          <mat-option value="FINALIZADA">Finalizada</mat-option>
          <mat-option value="CANCELADA">Cancelada</mat-option>
        </mat-select>
      </mat-form-field>

      <button mat-stroked-button (click)="cargar()" data-testid="btn-filtrar">
        <mat-icon>search</mat-icon> Buscar
      </button>
    </div>

    @if (cargando()) {
      <div class="spinner-container"><mat-spinner diameter="48" /></div>
    }

    @if (error()) {
      <p class="error-msg">{{ error() }}</p>
    }

    @if (!cargando() && !error()) {
      <mat-table [dataSource]="asignaciones()" class="asignaciones-table" data-testid="tabla-asignaciones">
        <ng-container matColumnDef="fecha">
          <mat-header-cell *matHeaderCellDef>Fecha</mat-header-cell>
          <mat-cell *matCellDef="let a">{{ a.fecha }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="periodo">
          <mat-header-cell *matHeaderCellDef>Período</mat-header-cell>
          <mat-cell *matCellDef="let a">{{ a.periodo }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="nombreCartera">
          <mat-header-cell *matHeaderCellDef>Cartera</mat-header-cell>
          <mat-cell *matCellDef="let a">{{ a.nombreCartera }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="nombreEjecutivo">
          <mat-header-cell *matHeaderCellDef>Ejecutivo</mat-header-cell>
          <mat-cell *matCellDef="let a">{{ a.nombreEjecutivo }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="cantidadPersonas">
          <mat-header-cell *matHeaderCellDef>Personas</mat-header-cell>
          <mat-cell *matCellDef="let a">{{ a.cantidadPersonas }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="estado">
          <mat-header-cell *matHeaderCellDef>Estado</mat-header-cell>
          <mat-cell *matCellDef="let a">
            <span [class]="'estado-badge estado-' + a.estado.toLowerCase()">{{ etiqueta(a.estado) }}</span>
          </mat-cell>
        </ng-container>

        <ng-container matColumnDef="acciones">
          <mat-header-cell *matHeaderCellDef></mat-header-cell>
          <mat-cell *matCellDef="let a">
            <button mat-icon-button color="primary" [routerLink]="[a.id]"
                    data-testid="btn-ver-detalle">
              <mat-icon>visibility</mat-icon>
            </button>
          </mat-cell>
        </ng-container>

        <mat-header-row *matHeaderRowDef="columnas" />
        <mat-row *matRowDef="let row; columns: columnas" />

        @if (asignaciones().length === 0) {
          <tr class="mat-mdc-row">
            <td class="sin-datos" [attr.colspan]="columnas.length">
              No hay asignaciones para los filtros seleccionados.
            </td>
          </tr>
        }
      </mat-table>
    }
  `,
  styles: `
    .page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
    .page-header h1 { margin: 0; }
    .filtros { display: flex; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; align-items: center; }
    .filtros mat-form-field { min-width: 180px; }
    .spinner-container { display: flex; justify-content: center; padding: 48px; }
    .asignaciones-table { width: 100%; }
    .error-msg { color: red; }
    .sin-datos { text-align: center; padding: 24px; color: #666; }
    .estado-badge { padding: 2px 8px; border-radius: 12px; font-size: 12px; font-weight: 500; }
    .estado-borrador { background: #e3f2fd; color: #1565c0; }
    .estado-publicada { background: #e8f5e9; color: #2e7d32; }
    .estado-finalizada { background: #f3e5f5; color: #6a1b9a; }
    .estado-cancelada { background: #fce4ec; color: #b71c1c; }
  `,
})
export class AsignacionesListComponent implements OnInit {
  private readonly service = inject(AsignacionesService);

  readonly columnas = ['fecha', 'periodo', 'nombreCartera', 'nombreEjecutivo', 'cantidadPersonas', 'estado', 'acciones'];
  readonly asignaciones = signal<ItemAsignacionDiariaAdmin[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly fechaCtrl = new FormControl('');
  readonly estadoCtrl = new FormControl('');

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.service.listarDiarias({
      fecha: this.fechaCtrl.value || undefined,
      estado: this.estadoCtrl.value || undefined,
    }).subscribe({
      next: (data) => { this.asignaciones.set(data); this.cargando.set(false); },
      error: () => { this.error.set('No se pudo cargar la lista de asignaciones.'); this.cargando.set(false); },
    });
  }

  etiqueta(estado: EstadoAsignacionDiaria): string {
    const etiquetas: Record<EstadoAsignacionDiaria, string> = {
      BORRADOR: 'Borrador', PUBLICADA: 'Publicada', FINALIZADA: 'Finalizada', CANCELADA: 'Cancelada',
    };
    return etiquetas[estado] ?? estado;
  }
}
