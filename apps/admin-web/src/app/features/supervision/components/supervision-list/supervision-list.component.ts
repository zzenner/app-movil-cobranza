import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { SupervisionService } from '../../services/supervision.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { ItemEjecutivoAdmin, ItemSupervisorAdmin } from '../../models/supervision.models';
import { AsignarSupervisorDialogComponent } from '../asignar-supervisor-dialog/asignar-supervisor-dialog.component';
import { ActualizarCodigoDialogComponent } from '../actualizar-codigo-dialog/actualizar-codigo-dialog.component';

@Component({
  selector: 'app-supervision-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDialogModule,
  ],
  template: `
    <div class="page-header">
      <h1>Supervisión</h1>
    </div>

    <div class="filtros">
      <mat-form-field appearance="outline">
        <mat-label>Buscar ejecutivo</mat-label>
        <input matInput [formControl]="buscarCtrl" placeholder="Nombre o usuario" />
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Supervisor</mat-label>
        <mat-select [formControl]="supervisorCtrl">
          <mat-option value="">Todos</mat-option>
          @for (s of supervisores(); track s.usuarioId) {
            <mat-option [value]="s.usuarioId">{{ s.nombreCompleto }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
    </div>

    @if (cargando()) {
      <div class="spinner-container"><mat-spinner diameter="48" /></div>
    }

    @if (error()) {
      <p class="error-msg">{{ error() }}</p>
    }

    @if (!cargando() && !error()) {
      <mat-table [dataSource]="ejecutivos()" class="supervision-table">
        <ng-container matColumnDef="nombreCompleto">
          <mat-header-cell *matHeaderCellDef>Ejecutivo</mat-header-cell>
          <mat-cell *matCellDef="let e">{{ e.nombreCompleto }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="nombreUsuario">
          <mat-header-cell *matHeaderCellDef>Usuario</mat-header-cell>
          <mat-cell *matCellDef="let e">{{ e.nombreUsuario }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="codigoEjecutivoOrigen">
          <mat-header-cell *matHeaderCellDef>Código</mat-header-cell>
          <mat-cell *matCellDef="let e">{{ e.codigoEjecutivoOrigen ?? '—' }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="supervisorNombre">
          <mat-header-cell *matHeaderCellDef>Supervisor</mat-header-cell>
          <mat-cell *matCellDef="let e">{{ e.supervisorNombre ?? '—' }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="acciones">
          <mat-header-cell *matHeaderCellDef></mat-header-cell>
          <mat-cell *matCellDef="let e">
            @if (puedeAdministrar()) {
              <button mat-icon-button color="primary"
                      matTooltip="{{ e.supervisorId ? 'Reasignar supervisor' : 'Asignar supervisor' }}"
                      (click)="abrirAsignar(e)">
                <mat-icon>person_add</mat-icon>
              </button>
              @if (e.supervisorId) {
                <button mat-icon-button color="warn" matTooltip="Quitar supervisor"
                        (click)="quitarSupervision(e)">
                  <mat-icon>person_remove</mat-icon>
                </button>
              }
              <button mat-icon-button matTooltip="Actualizar código"
                      (click)="abrirActualizarCodigo(e)">
                <mat-icon>tag</mat-icon>
              </button>
            }
          </mat-cell>
        </ng-container>

        <mat-header-row *matHeaderRowDef="columnas" />
        <mat-row *matRowDef="let row; columns: columnas" />
      </mat-table>
    }
  `,
  styles: `
    .page-header { display: flex; align-items: center; margin-bottom: 16px; }
    .page-header h1 { margin: 0; }
    .filtros { display: flex; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
    .filtros mat-form-field { min-width: 220px; }
    .spinner-container { display: flex; justify-content: center; padding: 48px; }
    .supervision-table { width: 100%; }
    .error-msg { color: red; }
  `,
})
export class SupervisionListComponent implements OnInit {
  private readonly service = inject(SupervisionService);
  private readonly authService = inject(AuthService);
  private readonly dialog = inject(MatDialog);

  readonly columnas = ['nombreCompleto', 'nombreUsuario', 'codigoEjecutivoOrigen', 'supervisorNombre', 'acciones'];

  readonly ejecutivos = signal<ItemEjecutivoAdmin[]>([]);
  readonly supervisores = signal<ItemSupervisorAdmin[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly puedeAdministrar = computed(() =>
    this.authService.profile()?.permisos?.includes('SUPERVISION_ADMINISTRAR') ?? false,
  );

  readonly buscarCtrl = new FormControl('');
  readonly supervisorCtrl = new FormControl('');

  ngOnInit(): void {
    this.cargarDatos();

    this.buscarCtrl.valueChanges.pipe(debounceTime(350), distinctUntilChanged()).subscribe(() => {
      this.cargarEjecutivos();
    });

    this.supervisorCtrl.valueChanges.subscribe(() => {
      this.cargarEjecutivos();
    });
  }

  cargarDatos(): void {
    this.service.listarSupervisores().subscribe({
      next: (data) => this.supervisores.set(data),
    });
    this.cargarEjecutivos();
  }

  cargarEjecutivos(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.service.listarEjecutivos({
      nombreUsuario: this.buscarCtrl.value ?? undefined,
      supervisorId: this.supervisorCtrl.value ?? undefined,
    }).subscribe({
      next: (data) => {
        this.ejecutivos.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la lista de ejecutivos.');
        this.cargando.set(false);
      },
    });
  }

  abrirAsignar(ejec: ItemEjecutivoAdmin): void {
    const dialogRef = this.dialog.open(AsignarSupervisorDialogComponent, {
      width: '400px',
      data: { ejecutivo: ejec, supervisores: this.supervisores() },
    });
    dialogRef.afterClosed().subscribe((supervisorId: string | undefined) => {
      if (supervisorId) {
        this.service.asignarOReaasignar(ejec.usuarioId, { supervisorId }).subscribe({
          next: () => this.cargarEjecutivos(),
          error: () => this.error.set('No se pudo asignar el supervisor.'),
        });
      }
    });
  }

  quitarSupervision(ejec: ItemEjecutivoAdmin): void {
    if (!confirm(`¿Quitar supervisor de ${ejec.nombreCompleto}?`)) return;
    this.service.quitarSupervision(ejec.usuarioId).subscribe({
      next: () => this.cargarEjecutivos(),
      error: () => this.error.set('No se pudo quitar la supervisión.'),
    });
  }

  abrirActualizarCodigo(ejec: ItemEjecutivoAdmin): void {
    const dialogRef = this.dialog.open(ActualizarCodigoDialogComponent, {
      width: '360px',
      data: { ejecutivo: ejec },
    });
    dialogRef.afterClosed().subscribe((codigo: string | null | undefined) => {
      if (codigo !== undefined) {
        this.service.actualizarCodigo(ejec.usuarioId, { codigo }).subscribe({
          next: () => this.cargarEjecutivos(),
          error: () => this.error.set('No se pudo actualizar el código.'),
        });
      }
    });
  }
}
