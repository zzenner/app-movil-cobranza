import {
  Component, OnInit, OnDestroy, inject, signal
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { interval, Subscription, switchMap, takeUntil } from 'rxjs';
import { Subject } from 'rxjs';
import { ImportacionService } from '../../services/importacion.service';
import {
  ErrorImportacion, EstadoImportacion,
  ImportacionDetalle, ESTADOS_EN_PROGRESO,
} from '../../models/importacion.models';

@Component({
  selector: 'app-importacion-detail',
  standalone: true,
  imports: [
    CommonModule, RouterModule, DatePipe,
    MatButtonModule, MatProgressSpinnerModule, MatIconModule,
    MatTableModule, MatPaginatorModule,
  ],
  template: `
    <div class="page-header">
      <h1>Importación {{ importacion()?.periodo ?? '' }}</h1>
      <a mat-button routerLink="/importacion">Volver</a>
    </div>

    @if (cargando()) {
      <div class="loading-container"><mat-spinner diameter="40" /></div>
    } @else if (error()) {
      <div class="error-container">
        <mat-icon color="warn">error</mat-icon>
        <p>Error al cargar. <button mat-button (click)="cargarDetalle()">Reintentar</button></p>
      </div>
    } @else if (importacion(); as im) {

      <div class="info-cards">
        <div class="info-card">
          <span class="label">Estado</span>
          <span [class]="'estado-badge estado-' + im.estado.toLowerCase()" data-testid="estado-badge">
            {{ im.estado }}
          </span>
        </div>
        <div class="info-card">
          <span class="label">Período</span>
          <span>{{ im.periodo }}</span>
        </div>
        <div class="info-card">
          <span class="label">Archivo</span>
          <span>{{ im.nombreArchivoOriginal }}</span>
        </div>
        <div class="info-card">
          <span class="label">Filas totales</span>
          <span>{{ im.filasTotales ?? '—' }}</span>
        </div>
        @if (im.filasRechazadas) {
          <div class="info-card error">
            <span class="label">Filas rechazadas</span>
            <span>{{ im.filasRechazadas }}</span>
          </div>
        }
        @if (im.filasProcesadas) {
          <div class="info-card">
            <span class="label">Filas procesadas</span>
            <span>{{ im.filasProcesadas }}</span>
          </div>
        }
        <div class="info-card">
          <span class="label">Fecha</span>
          <span>{{ im.fechaCreacion | date:'dd/MM/yyyy HH:mm' }}</span>
        </div>
      </div>

      @if (esEnProgreso(im.estado)) {
        <div class="spinner-estado" data-testid="spinner-progreso">
          <mat-spinner diameter="24" />
          <span>{{ mensajeEstado(im.estado) }}</span>
        </div>
      }

      @if (im.estado === 'VALIDADA') {
        <div class="panel-confirmar" data-testid="panel-confirmar">
          <p>La validación fue exitosa. Puede confirmar la importación para persistir los datos.</p>
          @if (im.filasAdvertencia) {
            <p class="advertencia">
              <mat-icon>warning</mat-icon>
              Hay {{ im.filasAdvertencia }} advertencias. Revise los errores antes de confirmar.
            </p>
          }
          <button mat-raised-button color="primary"
                  [disabled]="confirmando()"
                  (click)="confirmar()"
                  data-testid="btn-confirmar">
            @if (confirmando()) { <mat-spinner diameter="18" /> }
            Confirmar importación
          </button>
          @if (errorConfirmar()) {
            <div class="error-confirmar" data-testid="error-confirmar">{{ errorConfirmar() }}</div>
          }
        </div>
      }

      @if (im.estado === 'COMPLETADA') {
        <div class="panel-resultado" data-testid="panel-completada">
          <h3>Resultado del procesamiento</h3>
          <ul>
            <li>Personas creadas: {{ im.personasCreadas ?? 0 }}</li>
            <li>Personas actualizadas: {{ im.personasActualizadas ?? 0 }}</li>
            <li>Operaciones creadas: {{ im.operacionesCreadas ?? 0 }}</li>
            <li>Operaciones actualizadas: {{ im.operacionesActualizadas ?? 0 }}</li>
            <li>Cuotas creadas: {{ im.cuotasCreadas ?? 0 }}</li>
            <li>Cuotas actualizadas: {{ im.cuotasActualizadas ?? 0 }}</li>
          </ul>
        </div>
      }

      @if (im.estado === 'FALLIDA') {
        <div class="panel-fallida" data-testid="panel-fallida">
          <mat-icon color="warn">error</mat-icon>
          <p>La importación falló. Debe volver a subir el archivo corregido.</p>
          @if (im.mensajeError) {
            <p class="mensaje-error">{{ im.mensajeError }}</p>
          }
        </div>
      }

      @if (im.estado === 'EXPIRADA') {
        <div class="panel-expirada" data-testid="panel-expirada">
          <mat-icon>timer_off</mat-icon>
          <p>Esta importación expiró. Debe volver a subir el archivo.</p>
        </div>
      }

      @if (im.estado === 'CON_ERRORES' || im.estado === 'VALIDADA') {
        <div class="seccion-errores">
          <h3>Errores y advertencias</h3>
          @if (cargandoErrores()) {
            <mat-spinner diameter="24" />
          } @else if (errores().length === 0) {
            <p data-testid="sin-errores">No hay errores.</p>
          } @else {
            <table mat-table [dataSource]="errores()" data-testid="tabla-errores">
              <ng-container matColumnDef="fila">
                <th mat-header-cell *matHeaderCellDef>Fila</th>
                <td mat-cell *matCellDef="let e">{{ e.numeroFila ?? 'Global' }}</td>
              </ng-container>
              <ng-container matColumnDef="columna">
                <th mat-header-cell *matHeaderCellDef>Columna</th>
                <td mat-cell *matCellDef="let e">{{ e.columna ?? '—' }}</td>
              </ng-container>
              <ng-container matColumnDef="nivel">
                <th mat-header-cell *matHeaderCellDef>Nivel</th>
                <td mat-cell *matCellDef="let e">
                  <span [class]="'nivel-' + e.nivel.toLowerCase()">{{ e.nivel }}</span>
                </td>
              </ng-container>
              <ng-container matColumnDef="codigo">
                <th mat-header-cell *matHeaderCellDef>Código</th>
                <td mat-cell *matCellDef="let e">{{ e.codigoError }}</td>
              </ng-container>
              <ng-container matColumnDef="mensaje">
                <th mat-header-cell *matHeaderCellDef>Mensaje</th>
                <td mat-cell *matCellDef="let e">{{ e.mensaje }}</td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="columnasErrores"></tr>
              <tr mat-row *matRowDef="let row; columns: columnasErrores;"></tr>
            </table>
            <mat-paginator [length]="totalErrores()" [pageSize]="50"
                           (page)="onPaginarErrores($event)" />
          }
        </div>
      }
    }
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
    .loading-container, .error-container { display: flex; flex-direction: column; align-items: center; padding: 48px; gap: 16px; }
    .info-cards { display: flex; flex-wrap: wrap; gap: 16px; margin-bottom: 24px; }
    .info-card { background: #f5f5f5; border-radius: 8px; padding: 12px 16px; min-width: 140px; }
    .info-card.error { background: #ffebee; }
    .info-card .label { font-size: 11px; color: #757575; display: block; margin-bottom: 4px; }
    .estado-badge { padding: 2px 8px; border-radius: 12px; font-size: 13px; font-weight: 500; }
    .estado-recibida, .estado-validando, .estado-procesando { background: #e3f2fd; color: #1565c0; }
    .estado-validada { background: #e8f5e9; color: #2e7d32; }
    .estado-completada { background: #e8f5e9; color: #1b5e20; }
    .estado-con_errores { background: #fff8e1; color: #f57f17; }
    .estado-fallida { background: #ffebee; color: #c62828; }
    .estado-expirada { background: #eceff1; color: #546e7a; }
    .spinner-estado { display: flex; align-items: center; gap: 12px; padding: 16px; background: #e3f2fd; border-radius: 8px; margin-bottom: 16px; }
    .panel-confirmar { background: #e8f5e9; border-radius: 8px; padding: 16px; margin-bottom: 16px; display: flex; flex-direction: column; gap: 12px; }
    .panel-confirmar .advertencia { display: flex; align-items: center; gap: 8px; color: #f57f17; }
    .error-confirmar { color: #c62828; }
    .panel-resultado { background: #f5f5f5; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
    .panel-fallida, .panel-expirada { display: flex; align-items: center; gap: 12px; background: #ffebee; border-radius: 8px; padding: 16px; margin-bottom: 16px; flex-wrap: wrap; }
    .mensaje-error { font-size: 12px; color: #c62828; font-family: monospace; }
    .seccion-errores { margin-top: 24px; }
    .nivel-error { color: #c62828; font-weight: 500; }
    .nivel-advertencia { color: #f57f17; }
  `,
})
export class ImportacionDetailComponent implements OnInit, OnDestroy {
  private readonly service = inject(ImportacionService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroy$ = new Subject<void>();
  private pollingSub?: Subscription;

  readonly columnasErrores = ['fila', 'columna', 'nivel', 'codigo', 'mensaje'];

  readonly cargando = signal(true);
  readonly error = signal(false);
  readonly importacion = signal<ImportacionDetalle | null>(null);
  readonly cargandoErrores = signal(false);
  readonly errores = signal<ErrorImportacion[]>([]);
  readonly totalErrores = signal(0);
  readonly confirmando = signal(false);
  readonly errorConfirmar = signal<string | null>(null);

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      this.cargarDetalle(params['id']);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.detenerPolling();
  }

  cargarDetalle(id?: string): void {
    const importacionId = id ?? this.route.snapshot.params['id'];
    this.cargando.set(true);
    this.service.obtener(importacionId).subscribe({
      next: (im) => {
        this.importacion.set(im);
        this.cargando.set(false);
        if (im.estado === 'CON_ERRORES' || im.estado === 'VALIDADA') {
          this.cargarErrores(importacionId);
        }
        this.gestionarPolling(im.estado, importacionId);
      },
      error: () => { this.cargando.set(false); this.error.set(true); },
    });
  }

  private cargarErrores(id: string): void {
    this.cargandoErrores.set(true);
    this.service.listarErrores(id, 0, 50).subscribe({
      next: (r) => {
        this.errores.set(r.contenido);
        this.totalErrores.set(r.totalElementos);
        this.cargandoErrores.set(false);
      },
      error: () => this.cargandoErrores.set(false),
    });
  }

  private gestionarPolling(estado: EstadoImportacion, id: string): void {
    this.detenerPolling();
    if (ESTADOS_EN_PROGRESO.includes(estado)) {
      this.pollingSub = interval(3000)
        .pipe(takeUntil(this.destroy$), switchMap(() => this.service.obtener(id)))
        .subscribe({
          next: (im) => {
            this.importacion.set(im);
            if (!ESTADOS_EN_PROGRESO.includes(im.estado)) {
              this.detenerPolling();
              if (im.estado === 'CON_ERRORES' || im.estado === 'VALIDADA') {
                this.cargarErrores(id);
              }
            }
          },
        });
    }
  }

  private detenerPolling(): void {
    this.pollingSub?.unsubscribe();
    this.pollingSub = undefined;
  }

  confirmar(): void {
    if (this.confirmando()) return;
    const id = this.importacion()?.id;
    if (!id) return;

    this.confirmando.set(true);
    this.errorConfirmar.set(null);
    this.service.confirmar(id).subscribe({
      next: () => {
        this.confirmando.set(false);
        this.cargarDetalle(id);
      },
      error: (err) => {
        this.confirmando.set(false);
        const msg = err?.error?.detail ?? 'Error al confirmar la importación';
        this.errorConfirmar.set(msg);
      },
    });
  }

  esEnProgreso(estado: EstadoImportacion): boolean {
    return ESTADOS_EN_PROGRESO.includes(estado);
  }

  mensajeEstado(estado: EstadoImportacion): string {
    const mensajes: Partial<Record<EstadoImportacion, string>> = {
      RECIBIDA: 'Archivo recibido, esperando validación...',
      VALIDANDO: 'Validando archivo...',
      PROCESANDO: 'Procesando importación...',
    };
    return mensajes[estado] ?? '';
  }

  onPaginarErrores(event: PageEvent): void {
    const id = this.importacion()?.id;
    if (!id) return;
    this.cargandoErrores.set(true);
    this.service.listarErrores(id, event.pageIndex, event.pageSize).subscribe({
      next: (r) => { this.errores.set(r.contenido); this.cargandoErrores.set(false); },
      error: () => this.cargandoErrores.set(false),
    });
  }
}
