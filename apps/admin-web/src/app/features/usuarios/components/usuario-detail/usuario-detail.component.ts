import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpErrorResponse } from '@angular/common/http';
import { UsuariosService } from '../../services/usuarios.service';
import { DetalleUsuario } from '../../models/usuario.models';

@Component({
  selector: 'app-usuario-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    @if (cargando()) {
      <div class="loading-container">
        <mat-spinner diameter="40" />
      </div>
    } @else if (error404()) {
      <div class="error-container" data-testid="error-404">
        <mat-icon>person_off</mat-icon>
        <p>Usuario no encontrado.</p>
        <a mat-button color="primary" [routerLink]="['/usuarios']" [queryParams]="queryParamsVolver">
          <mat-icon>arrow_back</mat-icon> Volver al listado
        </a>
      </div>
    } @else if (errorRed()) {
      <div class="error-container" data-testid="error-red">
        <mat-icon color="warn">error</mat-icon>
        <p>Error de red al cargar el detalle.</p>
        <button mat-button (click)="cargar()">Reintentar</button>
        <a mat-button [routerLink]="['/usuarios']" [queryParams]="queryParamsVolver">Volver</a>
      </div>
    } @else if (usuario()) {
      <div class="detalle-container">
        <div class="detalle-header">
          <a mat-button [routerLink]="['/usuarios']" [queryParams]="queryParamsVolver">
            <mat-icon>arrow_back</mat-icon> Volver
          </a>
          <h1>{{ usuario()!.nombreUsuario }}</h1>
          <span [class]="'estado-badge estado-' + usuario()!.estadoCalculado.toLowerCase()">
            {{ usuario()!.estadoCalculado }}
          </span>
        </div>

        <mat-card>
          <mat-card-header>
            <mat-card-title>Datos personales</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="campo-grid">
              <div class="campo">
                <label>Nombre</label>
                <span>{{ usuario()!.nombres }} {{ usuario()!.apellidoPaterno }}
                  {{ usuario()!.apellidoMaterno ?? '' }}</span>
              </div>
              <div class="campo">
                <label>Correo</label>
                <span>{{ usuario()!.correo ?? '—' }}</span>
              </div>
              <div class="campo">
                <label>Activo</label>
                <span>{{ usuario()!.activo ? 'Sí' : 'No' }}</span>
              </div>
              <div class="campo">
                <label>Bloqueado</label>
                <span>{{ usuario()!.bloqueado ? 'Sí' : 'No' }}</span>
              </div>
              @if (usuario()!.bloqueadoHasta) {
                <div class="campo">
                  <label>Bloqueado hasta</label>
                  <span>{{ usuario()!.bloqueadoHasta | date:'dd/MM/yyyy HH:mm' }}</span>
                </div>
              }
              <div class="campo">
                <label>Intentos fallidos</label>
                <span>{{ usuario()!.intentosFallidos }}</span>
              </div>
              <div class="campo">
                <label>Último acceso</label>
                <span>{{ (usuario()!.fechaUltimoAcceso | date:'dd/MM/yyyy HH:mm') ?? '—' }}</span>
              </div>
            </div>
          </mat-card-content>
        </mat-card>

        <mat-card>
          <mat-card-header>
            <mat-card-title>Roles vigentes</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            @if (usuario()!.roles.length === 0) {
              <p class="sin-datos">Sin roles asignados.</p>
            } @else {
              @for (rol of usuario()!.roles; track rol.codigo) {
                <div class="rol-item">
                  <span class="chip-rol">{{ rol.codigo }}</span>
                  <span class="fecha-asignacion">desde {{ rol.fechaAsignacion | date:'dd/MM/yyyy' }}</span>
                </div>
              }
            }
          </mat-card-content>
        </mat-card>

        <mat-card>
          <mat-card-header>
            <mat-card-title>Permisos efectivos</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            @if (usuario()!.permisosEfectivos.length === 0) {
              <p class="sin-datos">Sin permisos.</p>
            } @else {
              <div class="chips-container">
                @for (permiso of usuario()!.permisosEfectivos; track permiso) {
                  <span class="chip-permiso">{{ permiso }}</span>
                }
              </div>
            }
          </mat-card-content>
        </mat-card>

        @if (usuario()!.supervisorId) {
          <mat-card>
            <mat-card-header>
              <mat-card-title>Supervisor</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <div class="campo">
                <label>Usuario supervisor</label>
                <span>{{ usuario()!.supervisorNombreUsuario }}</span>
              </div>
            </mat-card-content>
          </mat-card>
        }

        <mat-card>
          <mat-card-header>
            <mat-card-title>Fechas</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="campo-grid">
              <div class="campo">
                <label>Creado</label>
                <span>{{ usuario()!.fechaCreacion | date:'dd/MM/yyyy HH:mm' }}</span>
              </div>
              <div class="campo">
                <label>Actualizado</label>
                <span>{{ usuario()!.fechaActualizacion | date:'dd/MM/yyyy HH:mm' }}</span>
              </div>
            </div>
          </mat-card-content>
        </mat-card>
      </div>
    }
  `,
  styles: `
    .loading-container, .error-container {
      display: flex; flex-direction: column;
      align-items: center; padding: 48px; gap: 16px;
      color: rgba(0,0,0,.54);
    }
    .detalle-container { display: flex; flex-direction: column; gap: 16px; }
    .detalle-header {
      display: flex; align-items: center; gap: 16px; margin-bottom: 8px;
    }
    .detalle-header h1 { margin: 0; }
    .campo-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; }
    .campo label { display: block; font-size: 12px; color: rgba(0,0,0,.54); margin-bottom: 4px; }
    .rol-item { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
    .chip-rol {
      background: #e3f2fd; border-radius: 4px;
      padding: 2px 8px; font-size: 12px; font-weight: 500;
    }
    .chip-permiso {
      display: inline-block; background: #f3e5f5; border-radius: 4px;
      padding: 2px 8px; font-size: 12px; margin: 2px;
    }
    .chips-container { display: flex; flex-wrap: wrap; gap: 4px; }
    .fecha-asignacion { font-size: 13px; color: rgba(0,0,0,.54); }
    .sin-datos { color: rgba(0,0,0,.54); font-style: italic; }
    .estado-badge {
      padding: 4px 12px; border-radius: 12px; font-size: 13px; font-weight: 500;
    }
    .estado-activo { background: #e8f5e9; color: #2e7d32; }
    .estado-bloqueado_temporal { background: #fff8e1; color: #f57f17; }
    .estado-bloqueado { background: #ffebee; color: #c62828; }
    .estado-inactivo { background: #eceff1; color: #546e7a; }
  `,
})
export class UsuarioDetailComponent implements OnInit {
  private readonly service = inject(UsuariosService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly cargando = signal(false);
  readonly error404 = signal(false);
  readonly errorRed = signal(false);
  readonly usuario = signal<DetalleUsuario | null>(null);

  queryParamsVolver: Record<string, string | number> = {};

  ngOnInit(): void {
    this.queryParamsVolver = this.route.snapshot.queryParams;
    this.cargar();
  }

  cargar(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.cargando.set(true);
    this.error404.set(false);
    this.errorRed.set(false);
    this.service.obtenerDetalle(id).subscribe({
      next: (u) => {
        this.usuario.set(u);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        if (err.status === 403) {
          this.router.navigate(['/forbidden']);
        } else if (err.status === 404) {
          this.error404.set(true);
        } else {
          this.errorRed.set(true);
        }
      },
    });
  }
}
