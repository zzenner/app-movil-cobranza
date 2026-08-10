import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { Subject, takeUntil } from 'rxjs';
import { ImportacionService } from '../../services/importacion.service';
import { ItemCartera, ImportacionResumen } from '../../models/importacion.models';

@Component({
  selector: 'app-importacion-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatTableModule, MatPaginatorModule, MatFormFieldModule, MatSelectModule,
    MatButtonModule, MatProgressSpinnerModule, MatIconModule, DatePipe,
  ],
  template: `
    <div class="page-header">
      <h1>Importaciones mensuales</h1>
      <a mat-raised-button color="primary" routerLink="/importacion/nueva" data-testid="btn-nueva-importacion">
        <mat-icon>upload_file</mat-icon> Nueva importación
      </a>
    </div>

    <div class="filtros" [formGroup]="filtrosForm">
      <mat-form-field appearance="outline">
        <mat-label>Cartera</mat-label>
        <mat-select formControlName="carteraId">
          <mat-option value="">Todas</mat-option>
          @for (c of carteras(); track c.id) {
            <mat-option [value]="c.id">{{ c.nombre }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
    </div>

    @if (cargando()) {
      <div class="loading-container"><mat-spinner diameter="40" /></div>
    } @else if (error()) {
      <div class="error-container" data-testid="error-listado">
        <mat-icon color="warn">error</mat-icon>
        <p>Error al cargar. <button mat-button (click)="cargar()">Reintentar</button></p>
      </div>
    } @else if (totalElementos() === 0) {
      <div class="empty-container" data-testid="empty-listado">
        <mat-icon>cloud_upload</mat-icon>
        <p>No hay importaciones con los filtros actuales.</p>
      </div>
    } @else {
      <table mat-table [dataSource]="importaciones()" class="importaciones-table">
        <ng-container matColumnDef="periodo">
          <th mat-header-cell *matHeaderCellDef>Período</th>
          <td mat-cell *matCellDef="let i">{{ i.periodo }}</td>
        </ng-container>
        <ng-container matColumnDef="estado">
          <th mat-header-cell *matHeaderCellDef>Estado</th>
          <td mat-cell *matCellDef="let i">
            <span [class]="'estado-badge estado-' + i.estado.toLowerCase()">{{ i.estado }}</span>
          </td>
        </ng-container>
        <ng-container matColumnDef="archivo">
          <th mat-header-cell *matHeaderCellDef>Archivo</th>
          <td mat-cell *matCellDef="let i">{{ i.nombreArchivoOriginal }}</td>
        </ng-container>
        <ng-container matColumnDef="filas">
          <th mat-header-cell *matHeaderCellDef>Filas</th>
          <td mat-cell *matCellDef="let i">{{ i.filasTotales ?? '—' }}</td>
        </ng-container>
        <ng-container matColumnDef="fecha">
          <th mat-header-cell *matHeaderCellDef>Fecha</th>
          <td mat-cell *matCellDef="let i">{{ i.fechaCreacion | date:'dd/MM/yyyy HH:mm' }}</td>
        </ng-container>
        <ng-container matColumnDef="acciones">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let i">
            <a mat-button color="primary" [routerLink]="['/importacion', i.id]">Ver</a>
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="columnas"></tr>
        <tr mat-row *matRowDef="let row; columns: columnas;"></tr>
      </table>

      <mat-paginator
        [length]="totalElementos()"
        [pageSize]="tamanio"
        [pageIndex]="pagina()"
        (page)="onPaginar($event)"
        showFirstLastButtons />
    }
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
    .filtros { display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 16px; }
    .filtros mat-form-field { min-width: 220px; }
    .loading-container, .error-container, .empty-container {
      display: flex; flex-direction: column; align-items: center; padding: 48px; gap: 16px;
    }
    .importaciones-table { width: 100%; }
    .estado-badge { padding: 2px 8px; border-radius: 12px; font-size: 12px; font-weight: 500; }
    .estado-recibida, .estado-validando, .estado-procesando { background: #e3f2fd; color: #1565c0; }
    .estado-validada { background: #e8f5e9; color: #2e7d32; }
    .estado-completada { background: #e8f5e9; color: #1b5e20; }
    .estado-con_errores { background: #fff8e1; color: #f57f17; }
    .estado-fallida { background: #ffebee; color: #c62828; }
    .estado-expirada { background: #eceff1; color: #546e7a; }
  `,
})
export class ImportacionListComponent implements OnInit, OnDestroy {
  private readonly service = inject(ImportacionService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroy$ = new Subject<void>();

  readonly columnas = ['periodo', 'estado', 'archivo', 'filas', 'fecha', 'acciones'];
  readonly tamanio = 20;

  readonly cargando = signal(false);
  readonly error = signal(false);
  readonly importaciones = signal<ImportacionResumen[]>([]);
  readonly totalElementos = signal(0);
  readonly pagina = signal(0);
  readonly carteras = signal<ItemCartera[]>([]);

  readonly filtrosForm = new FormGroup({ carteraId: new FormControl('') });

  ngOnInit(): void {
    this.service.listarCarteras().subscribe({ next: (c) => this.carteras.set(c) });

    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const p = parseInt(params['pagina'] ?? '0', 10);
      this.pagina.set(isNaN(p) ? 0 : p);
      this.filtrosForm.setValue({ carteraId: params['carteraId'] ?? '' }, { emitEvent: false });
      this.cargar();
    });

    this.filtrosForm.get('carteraId')!.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.router.navigate([], { relativeTo: this.route,
        queryParams: { pagina: 0, carteraId: this.filtrosForm.value.carteraId || undefined } }));
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    const carteraId = this.filtrosForm.value.carteraId || undefined;
    this.service.listar(this.pagina(), this.tamanio, carteraId).subscribe({
      next: (r) => {
        this.importaciones.set(r.contenido);
        this.totalElementos.set(r.totalElementos);
        this.cargando.set(false);
      },
      error: () => { this.cargando.set(false); this.error.set(true); },
    });
  }

  onPaginar(event: PageEvent): void {
    this.router.navigate([], { relativeTo: this.route,
      queryParams: { pagina: event.pageIndex,
        carteraId: this.filtrosForm.value.carteraId || undefined } });
  }
}
