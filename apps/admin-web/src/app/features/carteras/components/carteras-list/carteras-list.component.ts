import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { CarterasService } from '../../services/carteras.service';
import { ItemCatalogoCartera } from '../../models/cartera.models';

@Component({
  selector: 'app-carteras-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatIconModule,
  ],
  template: `
    <div class="page-header">
      <h1>Carteras</h1>
    </div>

    @if (cargando()) {
      <div class="spinner-container">
        <mat-spinner diameter="48" />
      </div>
    }

    @if (error()) {
      <p class="error-msg">{{ error() }}</p>
    }

    @if (!cargando() && !error()) {
      <mat-table [dataSource]="carteras()" class="carteras-table">
        <ng-container matColumnDef="codigoOrigen">
          <mat-header-cell *matHeaderCellDef>Código</mat-header-cell>
          <mat-cell *matCellDef="let c">{{ c.codigoOrigen ?? '—' }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="nombre">
          <mat-header-cell *matHeaderCellDef>Nombre</mat-header-cell>
          <mat-cell *matCellDef="let c">{{ c.nombre }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="descripcion">
          <mat-header-cell *matHeaderCellDef>Descripción</mat-header-cell>
          <mat-cell *matCellDef="let c">{{ c.descripcion ?? '—' }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="activa">
          <mat-header-cell *matHeaderCellDef>Estado</mat-header-cell>
          <mat-cell *matCellDef="let c">
            <mat-chip [color]="c.activa ? 'primary' : 'warn'" highlighted>
              {{ c.activa ? 'Activa' : 'Inactiva' }}
            </mat-chip>
          </mat-cell>
        </ng-container>

        <mat-header-row *matHeaderRowDef="columnas" />
        <mat-row *matRowDef="let row; columns: columnas" />
      </mat-table>
    }
  `,
  styles: `
    .page-header { display: flex; align-items: center; margin-bottom: 24px; }
    .page-header h1 { margin: 0; }
    .spinner-container { display: flex; justify-content: center; padding: 48px; }
    .carteras-table { width: 100%; }
    .error-msg { color: red; }
  `,
})
export class CarterasListComponent implements OnInit {
  private readonly service = inject(CarterasService);

  readonly columnas = ['codigoOrigen', 'nombre', 'descripcion', 'activa'];
  readonly carteras = signal<ItemCatalogoCartera[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.service.listarCatalogo().subscribe({
      next: (data) => {
        this.carteras.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el catálogo de carteras.');
        this.cargando.set(false);
      },
    });
  }
}
