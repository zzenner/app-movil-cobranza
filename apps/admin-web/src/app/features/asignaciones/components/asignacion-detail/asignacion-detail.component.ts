import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { AsignacionesService } from '../../services/asignaciones.service';
import { DetalleAsignacionDiariaAdmin, EstadoAsignacionDiaria } from '../../models/asignacion.models';

@Component({
  selector: 'app-asignacion-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatCardModule,
    MatDialogModule,
  ],
  template: `
    <div class="page-header">
      <button mat-icon-button routerLink="/asignaciones" data-testid="btn-volver">
        <mat-icon>arrow_back</mat-icon>
      </button>
      <h1>Detalle de Asignación</h1>
    </div>

    @if (cargando()) {
      <div class="spinner-container"><mat-spinner diameter="48" /></div>
    }

    @if (error()) {
      <p class="error-msg">{{ error() }}</p>
    }

    @if (detalle()) {
      <div class="detalle-container">
        <!-- Info principal -->
        <mat-card>
          <mat-card-header>
            <mat-card-title>
              {{ detalle()!.fecha }} — {{ detalle()!.nombreCartera }}
              <span [class]="'estado-badge estado-' + detalle()!.estado.toLowerCase()" data-testid="badge-estado">
                {{ etiqueta(detalle()!.estado) }}
              </span>
            </mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="info-grid">
              <div class="info-item"><label>Período</label><span>{{ detalle()!.periodo }}</span></div>
              <div class="info-item"><label>Cartera</label><span>{{ detalle()!.nombreCartera }}</span></div>
              <div class="info-item"><label>Supervisor</label><span>{{ detalle()!.nombreSupervisor }}</span></div>
              <div class="info-item"><label>Ejecutivo</label><span>{{ detalle()!.nombreEjecutivo }}</span></div>
              <div class="info-item"><label>Personas</label><span>{{ detalle()!.cantidadPersonas }}</span></div>
              @if (detalle()!.fechaPublicacion) {
                <div class="info-item"><label>Publicada</label><span>{{ detalle()!.fechaPublicacion | date:'dd/MM/yyyy HH:mm' }}</span></div>
                <div class="info-item"><label>Publicada por</label><span>{{ detalle()!.nombrePublicador ?? '—' }}</span></div>
              }
              @if (detalle()!.motivoCancelacion) {
                <div class="info-item full-width"><label>Motivo cancelación</label><span>{{ detalle()!.motivoCancelacion }}</span></div>
              }
            </div>
          </mat-card-content>
          <mat-card-actions>
            @if (detalle()!.estado === 'BORRADOR') {
              <button mat-raised-button color="primary" (click)="publicar()"
                      [disabled]="procesando()" data-testid="btn-publicar">
                @if (procesando()) {
                  <mat-spinner diameter="18" />
                } @else {
                  <mat-icon>publish</mat-icon>
                }
                Publicar asignación
              </button>
              <button mat-stroked-button color="warn" (click)="cancelar()"
                      [disabled]="procesando()" data-testid="btn-cancelar">
                <mat-icon>cancel</mat-icon> Cancelar borrador
              </button>
            }
            @if (detalle()!.estado === 'PUBLICADA') {
              <button mat-stroked-button color="warn" (click)="cancelar()"
                      [disabled]="procesando()" data-testid="btn-cancelar-publicada">
                <mat-icon>cancel</mat-icon> Cancelar asignación
              </button>
            }
          </mat-card-actions>
        </mat-card>

        @if (mensajeExito()) {
          <div class="exito-banner" role="status" data-testid="banner-exito">
            <mat-icon>check_circle</mat-icon>
            {{ mensajeExito() }}
          </div>
        }

        @if (errorAccion()) {
          <div class="error-banner" role="alert" data-testid="banner-error">
            <mat-icon>error</mat-icon>
            {{ errorAccion() }}
          </div>
        }

        <!-- Lista de personas -->
        <mat-card>
          <mat-card-header>
            <mat-card-title>Personas incluidas ({{ detalle()!.personas.length }})</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            @if (detalle()!.personas.length === 0) {
              <p class="sin-personas">Sin personas en esta asignación.</p>
            }
            @for (p of detalle()!.personas; track p.personaId) {
              <div class="persona-fila" data-testid="fila-persona">
                <strong>{{ p.nombre }}</strong>
                <span class="rut">{{ p.rutNumero }}-{{ p.rutDv }}</span>
              </div>
              <mat-divider />
            }
          </mat-card-content>
        </mat-card>
      </div>
    }
  `,
  styles: `
    .page-header { display: flex; align-items: center; gap: 8px; margin-bottom: 24px; }
    .page-header h1 { margin: 0; }
    .spinner-container { display: flex; justify-content: center; padding: 48px; }
    .error-msg { color: red; }
    .detalle-container { display: flex; flex-direction: column; gap: 16px; max-width: 800px; }
    .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; padding: 8px 0; }
    .info-item { display: flex; flex-direction: column; gap: 2px; }
    .info-item.full-width { grid-column: 1 / -1; }
    .info-item label { font-size: 12px; color: #666; }
    .info-item span { font-size: 14px; }
    .estado-badge { padding: 2px 10px; border-radius: 12px; font-size: 13px; font-weight: 500; margin-left: 12px; }
    .estado-borrador { background: #e3f2fd; color: #1565c0; }
    .estado-publicada { background: #e8f5e9; color: #2e7d32; }
    .estado-finalizada { background: #f3e5f5; color: #6a1b9a; }
    .estado-cancelada { background: #fce4ec; color: #b71c1c; }
    .exito-banner { background: #e8f5e9; color: #2e7d32; padding: 12px 16px; border-radius: 4px;
                    display: flex; align-items: center; gap: 8px; }
    .error-banner { background: #fce4ec; color: #b71c1c; padding: 12px 16px; border-radius: 4px;
                    display: flex; align-items: center; gap: 8px; }
    .persona-fila { display: flex; align-items: center; gap: 16px; padding: 8px 0; }
    .rut { color: #666; font-size: 13px; }
    .sin-personas { color: #888; padding: 16px 0; }
  `,
})
export class AsignacionDetailComponent implements OnInit {
  private readonly service = inject(AsignacionesService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly detalle = signal<DetalleAsignacionDiariaAdmin | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly procesando = signal(false);
  readonly errorAccion = signal<string | null>(null);
  readonly mensajeExito = signal<string | null>(null);

  get id(): string {
    return this.route.snapshot.paramMap.get('id')!;
  }

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.service.obtenerDetalle(this.id).subscribe({
      next: (data) => { this.detalle.set(data); this.cargando.set(false); },
      error: () => { this.error.set('No se pudo cargar el detalle.'); this.cargando.set(false); },
    });
  }

  publicar(): void {
    const confirmado = confirm(
      'Esta asignación quedará disponible para descarga en el dispositivo del Ejecutivo.\n\n¿Desea publicar?'
    );
    if (!confirmado) return;

    this.procesando.set(true);
    this.errorAccion.set(null);
    this.mensajeExito.set(null);

    this.service.publicar(this.id).subscribe({
      next: () => {
        this.mensajeExito.set('Asignación publicada correctamente.');
        this.procesando.set(false);
        this.cargar();
      },
      error: (err) => {
        this.procesando.set(false);
        this.errorAccion.set(err?.error?.detail ?? 'No se pudo publicar la asignación.');
      },
    });
  }

  cancelar(): void {
    const motivo = prompt('Ingrese el motivo de cancelación:');
    if (!motivo?.trim()) return;

    this.procesando.set(true);
    this.errorAccion.set(null);
    this.mensajeExito.set(null);

    this.service.cancelar(this.id, { motivo: motivo.trim() }).subscribe({
      next: () => {
        this.mensajeExito.set('Asignación cancelada.');
        this.procesando.set(false);
        this.cargar();
      },
      error: (err) => {
        this.procesando.set(false);
        this.errorAccion.set(err?.error?.detail ?? 'No se pudo cancelar la asignación.');
      },
    });
  }

  etiqueta(estado: EstadoAsignacionDiaria): string {
    const etiquetas: Record<EstadoAsignacionDiaria, string> = {
      BORRADOR: 'Borrador', PUBLICADA: 'Publicada', FINALIZADA: 'Finalizada', CANCELADA: 'Cancelada',
    };
    return etiquetas[estado] ?? estado;
  }
}
