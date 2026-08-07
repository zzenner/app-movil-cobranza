import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpErrorResponse } from '@angular/common/http';
import { UsuariosService } from '../../services/usuarios.service';

@Component({
  selector: 'app-usuario-edit',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  template: `
    @if (cargando()) {
      <div class="loading-container">
        <mat-spinner diameter="40" />
      </div>
    } @else if (error404()) {
      <div class="error-container" data-testid="error-404">
        <p>Usuario no encontrado.</p>
        <a mat-button routerLink="/usuarios">Volver</a>
      </div>
    } @else {
      <div class="edit-container">
        <div class="edit-header">
          <a mat-button [routerLink]="['/usuarios', id()]">
            <mat-icon>arrow_back</mat-icon> Volver al detalle
          </a>
          <h1>Editar: {{ nombreUsuario() }}</h1>
        </div>

        @if (errorGeneral()) {
          <div class="error-banner" data-testid="error-general">
            {{ errorGeneral() }}
          </div>
        }

        <form [formGroup]="form" (ngSubmit)="guardar()">
          <mat-card>
            <mat-card-header>
              <mat-card-title>Datos personales</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <mat-form-field appearance="outline" class="campo-ancho">
                <mat-label>Nombre de usuario</mat-label>
                <input matInput [value]="nombreUsuario()" readonly />
                <mat-hint>El nombre de usuario no puede modificarse</mat-hint>
              </mat-form-field>

              <mat-form-field appearance="outline" class="campo-ancho">
                <mat-label>Nombres</mat-label>
                <input matInput formControlName="nombres" />
                @if (form.get('nombres')?.hasError('required')) {
                  <mat-error>Campo obligatorio</mat-error>
                }
              </mat-form-field>

              <mat-form-field appearance="outline" class="campo-ancho">
                <mat-label>Apellido paterno</mat-label>
                <input matInput formControlName="apellidoPaterno" />
                @if (form.get('apellidoPaterno')?.hasError('required')) {
                  <mat-error>Campo obligatorio</mat-error>
                }
              </mat-form-field>

              <mat-form-field appearance="outline" class="campo-ancho">
                <mat-label>Apellido materno (opcional)</mat-label>
                <input matInput formControlName="apellidoMaterno" />
              </mat-form-field>

              <mat-form-field appearance="outline" class="campo-ancho">
                <mat-label>Correo electrónico (opcional)</mat-label>
                <input matInput formControlName="correo" type="email" />
                @if (form.get('correo')?.hasError('email')) {
                  <mat-error>Formato de correo inválido</mat-error>
                }
              </mat-form-field>
            </mat-card-content>
          </mat-card>

          <div class="acciones">
            <button mat-button type="button" [routerLink]="['/usuarios', id()]">Cancelar</button>
            <button mat-raised-button color="primary" type="submit"
                    [disabled]="guardando()">
              @if (guardando()) {
                <mat-spinner diameter="18" />
              } @else {
                Guardar cambios
              }
            </button>
          </div>
        </form>
      </div>
    }
  `,
  styles: `
    .loading-container, .error-container {
      display: flex; flex-direction: column;
      align-items: center; padding: 48px; gap: 16px;
    }
    .edit-container { display: flex; flex-direction: column; gap: 16px; }
    .edit-header { display: flex; align-items: center; gap: 16px; margin-bottom: 8px; }
    .edit-header h1 { margin: 0; }
    .campo-ancho { width: 100%; max-width: 480px; display: block; margin-bottom: 8px; }
    .acciones { display: flex; gap: 8px; justify-content: flex-end; padding: 16px 0; }
    .error-banner {
      background: #ffebee; color: #c62828; padding: 12px 16px;
      border-radius: 4px; border-left: 4px solid #c62828;
    }
  `,
})
export class UsuarioEditComponent implements OnInit {
  private readonly service = inject(UsuariosService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly error404 = signal(false);
  readonly errorGeneral = signal<string | null>(null);
  readonly id = signal('');
  readonly nombreUsuario = signal('');
  private version = 0;

  readonly form = this.fb.nonNullable.group({
    nombres: ['', Validators.required],
    apellidoPaterno: ['', Validators.required],
    apellidoMaterno: [''],
    correo: ['', Validators.email],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.id.set(id);
    this.service.obtenerDetalle(id).subscribe({
      next: (u) => {
        this.nombreUsuario.set(u.nombreUsuario);
        this.version = u.version;
        this.form.patchValue({
          nombres: u.nombres,
          apellidoPaterno: u.apellidoPaterno,
          apellidoMaterno: u.apellidoMaterno ?? '',
          correo: u.correo ?? '',
        });
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        if (err.status === 404) {
          this.error404.set(true);
        }
      },
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.errorGeneral.set(null);
    const v = this.form.getRawValue();

    this.service.actualizarDatosBasicos(this.id(), {
      nombres: v.nombres,
      apellidoPaterno: v.apellidoPaterno,
      apellidoMaterno: v.apellidoMaterno || null,
      correo: v.correo || null,
      version: this.version,
    }).subscribe({
      next: () => this.router.navigate(['/usuarios', this.id()]),
      error: (err: HttpErrorResponse) => {
        this.guardando.set(false);
        const code = err.error?.code;
        if (code === 'CONFLICTO_VERSION') {
          this.errorGeneral.set('Los datos fueron modificados por otro administrador. Vuelva a cargar la página.');
        } else if (code === 'CORREO_DUPLICADO') {
          this.errorGeneral.set('El correo electrónico ya está registrado en otro usuario.');
        } else {
          this.errorGeneral.set('Error al guardar los cambios. Intente nuevamente.');
        }
      },
    });
  }
}
