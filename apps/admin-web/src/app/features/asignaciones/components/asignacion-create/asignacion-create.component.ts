import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatStepperModule } from '@angular/material/stepper';
import { MatCardModule } from '@angular/material/card';
import { AsignacionesService } from '../../services/asignaciones.service';
import {
  ItemAsignacionMensualAdmin,
  ItemPersonaDisponible,
} from '../../models/asignacion.models';

@Component({
  selector: 'app-asignacion-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatStepperModule,
    MatCardModule,
  ],
  template: `
    <div class="page-header">
      <h1>Nueva Asignación Diaria</h1>
    </div>

    @if (error()) {
      <div class="error-banner" role="alert">
        <mat-icon>error</mat-icon>
        {{ error() }}
      </div>
    }

    <mat-stepper orientation="vertical" [linear]="true" #stepper>

      <!-- Paso 1: Período y mensual -->
      <mat-step [stepControl]="paso1" label="Seleccionar período y cartera">
        <form [formGroup]="paso1">
          <div class="step-content">
            <mat-form-field appearance="outline">
              <mat-label>Período</mat-label>
              <mat-select formControlName="periodo" (selectionChange)="onPeriodoCambia()" data-testid="sel-periodo">
                @for (p of periodos(); track p.periodo) {
                  <mat-option [value]="p.periodo">{{ p.periodo }}</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Ejecutivo / Cartera</mat-label>
              <mat-select formControlName="mensualId" data-testid="sel-mensual">
                @for (m of mensualesFiltrados(); track m.id) {
                  <mat-option [value]="m.id">{{ m.nombreEjecutivo }} — {{ m.nombreCartera }} ({{ m.cantidadPersonas }} personas)</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Fecha de asignación</mat-label>
              <input matInput type="date" formControlName="fecha" data-testid="input-fecha" />
            </mat-form-field>
          </div>
          <div class="step-actions">
            <button mat-raised-button color="primary" matStepperNext
                    [disabled]="paso1.invalid" (click)="cargarPersonas()"
                    data-testid="btn-siguiente-paso1">
              Siguiente
            </button>
          </div>
        </form>
      </mat-step>

      <!-- Paso 2: Selección de personas -->
      <mat-step label="Seleccionar personas">
        <div class="step-content">
          @if (cargandoPersonas()) {
            <div class="spinner-container"><mat-spinner diameter="36" /></div>
          }

          @if (!cargandoPersonas()) {
            <p class="seleccionadas-count" data-testid="contador-seleccionadas">
              Seleccionadas: {{ personasSeleccionadas().size }}
            </p>

            <div class="personas-lista">
              @for (p of personas(); track p.personaId) {
                <mat-card class="persona-card" [class.ya-asignada]="p.tieneAsignacionDiaria"
                          (click)="togglePersona(p.personaId)" data-testid="persona-card">
                  <div class="persona-row">
                    <mat-checkbox
                      [checked]="personasSeleccionadas().has(p.personaId)"
                      [disabled]="p.tieneAsignacionDiaria"
                      (change)="togglePersona(p.personaId)"
                      (click)="$event.stopPropagation()"
                    />
                    <div class="persona-info">
                      <strong>{{ p.nombre }}</strong>
                      <span class="rut">{{ p.rutNumero }}-{{ p.rutDv }}</span>
                      <span class="ops">{{ p.cantidadOperaciones }} op.</span>
                      @if (p.tieneAsignacionDiaria) {
                        <span class="badge-asignada">Ya asignada</span>
                      }
                    </div>
                  </div>
                </mat-card>
              }

              @if (personas().length === 0) {
                <p class="sin-personas">No hay personas disponibles en este período/cartera.</p>
              }
            </div>
          }
        </div>

        <div class="step-actions">
          <button mat-stroked-button matStepperPrevious>Atrás</button>
          <button mat-raised-button color="primary" (click)="guardarBorrador()"
                  [disabled]="personasSeleccionadas().size === 0 || guardando()"
                  data-testid="btn-guardar-borrador">
            @if (guardando()) { <mat-spinner diameter="18" /> }
            @else { Guardar borrador }
          </button>
        </div>
      </mat-step>

    </mat-stepper>
  `,
  styles: `
    .page-header { margin-bottom: 24px; }
    .page-header h1 { margin: 0; }
    .step-content { display: flex; flex-direction: column; gap: 12px; padding: 12px 0; max-width: 600px; }
    .step-content mat-form-field { width: 100%; }
    .step-actions { display: flex; gap: 12px; margin-top: 8px; }
    .spinner-container { display: flex; justify-content: center; padding: 24px; }
    .error-banner { background: #fce4ec; color: #b71c1c; padding: 12px 16px; border-radius: 4px;
                    display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
    .seleccionadas-count { font-weight: 500; margin-bottom: 8px; }
    .personas-lista { display: flex; flex-direction: column; gap: 6px; max-height: 400px; overflow-y: auto; }
    .persona-card { cursor: pointer; padding: 8px 12px !important; }
    .persona-card.ya-asignada { opacity: 0.5; cursor: default; }
    .persona-row { display: flex; align-items: center; gap: 12px; }
    .persona-info { display: flex; flex-direction: column; gap: 2px; }
    .persona-info strong { font-size: 14px; }
    .rut { font-size: 12px; color: #666; }
    .ops { font-size: 12px; color: #888; }
    .badge-asignada { font-size: 11px; background: #ffe082; padding: 1px 6px; border-radius: 8px; color: #5d4037; }
    .sin-personas { color: #888; padding: 24px 0; text-align: center; }
  `,
})
export class AsignacionCreateComponent implements OnInit {
  private readonly service = inject(AsignacionesService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly periodos = signal<{ periodo: string }[]>([]);
  readonly mensuales = signal<ItemAsignacionMensualAdmin[]>([]);
  readonly mensualesFiltrados = signal<ItemAsignacionMensualAdmin[]>([]);
  readonly personas = signal<ItemPersonaDisponible[]>([]);
  readonly personasSeleccionadas = signal<Set<string>>(new Set());
  readonly cargandoPersonas = signal(false);
  readonly guardando = signal(false);
  readonly error = signal<string | null>(null);

  readonly paso1: FormGroup = this.fb.group({
    periodo: ['', Validators.required],
    mensualId: ['', Validators.required],
    fecha: ['', Validators.required],
  });

  ngOnInit(): void {
    this.service.listarPeriodos().subscribe({
      next: (data) => this.periodos.set(data),
    });
    this.service.listarMensuales().subscribe({
      next: (data) => this.mensuales.set(data),
    });
  }

  onPeriodoCambia(): void {
    const periodo = this.paso1.get('periodo')?.value;
    this.mensualesFiltrados.set(
      periodo ? this.mensuales().filter((m) => m.periodo === periodo) : this.mensuales()
    );
    this.paso1.patchValue({ mensualId: '' });
  }

  cargarPersonas(): void {
    const mensualId = this.paso1.get('mensualId')?.value;
    if (!mensualId) return;
    this.cargandoPersonas.set(true);
    this.personasSeleccionadas.set(new Set());
    this.service.listarPersonasDisponibles(mensualId).subscribe({
      next: (data) => { this.personas.set(data); this.cargandoPersonas.set(false); },
      error: () => { this.error.set('No se pudo cargar la lista de personas.'); this.cargandoPersonas.set(false); },
    });
  }

  togglePersona(personaId: string): void {
    const actual = new Set(this.personasSeleccionadas());
    if (actual.has(personaId)) actual.delete(personaId);
    else actual.add(personaId);
    this.personasSeleccionadas.set(actual);
  }

  guardarBorrador(): void {
    if (this.paso1.invalid || this.guardando()) return;
    this.guardando.set(true);
    this.error.set(null);

    this.service.crearBorrador({
      asignacionMensualId: this.paso1.get('mensualId')!.value,
      fecha: this.paso1.get('fecha')!.value,
      personaIds: [...this.personasSeleccionadas()],
    }).subscribe({
      next: (resp) => this.router.navigate(['/asignaciones', resp.id]),
      error: (err) => {
        this.guardando.set(false);
        this.error.set(err?.error?.detail ?? 'Error al guardar el borrador.');
      },
    });
  }
}
