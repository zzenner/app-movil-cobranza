import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpErrorResponse } from '@angular/common/http';
import { UsuariosService } from '../../services/usuarios.service';
import { ItemRol } from '../../models/usuario.models';

@Component({
  selector: 'app-usuario-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="create-container">
      <div class="create-header">
        <a mat-button routerLink="/usuarios">
          <mat-icon>arrow_back</mat-icon> Volver
        </a>
        <h1>Nuevo usuario</h1>
      </div>

      @if (errorGeneral()) {
        <div class="error-banner" data-testid="error-general">
          {{ errorGeneral() }}
        </div>
      }

      <form [formGroup]="form" (ngSubmit)="guardar()">
        <mat-card>
          <mat-card-header>
            <mat-card-title>Datos de acceso</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <mat-form-field appearance="outline" class="campo-ancho">
              <mat-label>Nombre de usuario</mat-label>
              <input matInput formControlName="nombreUsuario" autocomplete="off" />
              @if (form.get('nombreUsuario')?.hasError('required')) {
                <mat-error>Campo obligatorio</mat-error>
              } @else if (form.get('nombreUsuario')?.hasError('maxlength')) {
                <mat-error>Máximo 50 caracteres</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="campo-ancho">
              <mat-label>Contraseña inicial</mat-label>
              <input matInput
                     [type]="mostrarContrasena() ? 'text' : 'password'"
                     formControlName="contrasena"
                     autocomplete="new-password" />
              <button mat-icon-button matSuffix type="button"
                      (click)="mostrarContrasena.set(!mostrarContrasena())">
                <mat-icon>{{ mostrarContrasena() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (form.get('contrasena')?.hasError('required')) {
                <mat-error>Campo obligatorio</mat-error>
              } @else if (form.get('contrasena')?.hasError('minlength')) {
                <mat-error>Mínimo 8 caracteres</mat-error>
              }
            </mat-form-field>
          </mat-card-content>
        </mat-card>

        <mat-card>
          <mat-card-header>
            <mat-card-title>Datos personales</mat-card-title>
          </mat-card-header>
          <mat-card-content>
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
              <input matInput formControlName="correo" type="email" autocomplete="off" />
              @if (form.get('correo')?.hasError('email')) {
                <mat-error>Formato de correo inválido</mat-error>
              }
            </mat-form-field>
          </mat-card-content>
        </mat-card>

        <mat-card>
          <mat-card-header>
            <mat-card-title>Roles iniciales</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            @if (cargandoRoles()) {
              <mat-spinner diameter="24" />
            } @else if (roles().length === 0) {
              <p class="sin-datos">No hay roles disponibles.</p>
            } @else {
              <div class="roles-list">
                @for (rol of roles(); track rol.id) {
                  <mat-checkbox [checked]="rolesSeleccionados().has(rol.codigo)"
                                (change)="toggleRol(rol.codigo, $event.checked)">
                    {{ rol.nombre }} ({{ rol.codigo }})
                  </mat-checkbox>
                }
              </div>
              @if (sinRolesError()) {
                <mat-error>Seleccione al menos un rol</mat-error>
              }
            }
          </mat-card-content>
        </mat-card>

        <div class="acciones">
          <button mat-button type="button" routerLink="/usuarios">Cancelar</button>
          <button mat-raised-button color="primary" type="submit"
                  [disabled]="guardando()">
            @if (guardando()) {
              <mat-spinner diameter="18" />
            } @else {
              Crear usuario
            }
          </button>
        </div>
      </form>
    </div>
  `,
  styles: `
    .create-container { display: flex; flex-direction: column; gap: 16px; }
    .create-header { display: flex; align-items: center; gap: 16px; margin-bottom: 8px; }
    .create-header h1 { margin: 0; }
    .campo-ancho { width: 100%; max-width: 480px; display: block; margin-bottom: 8px; }
    .roles-list { display: flex; flex-direction: column; gap: 8px; }
    .acciones { display: flex; gap: 8px; justify-content: flex-end; padding: 16px 0; }
    .sin-datos { color: rgba(0,0,0,.54); font-style: italic; }
    .error-banner {
      background: #ffebee; color: #c62828; padding: 12px 16px;
      border-radius: 4px; border-left: 4px solid #c62828;
    }
  `,
})
export class UsuarioCreateComponent implements OnInit {
  private readonly service = inject(UsuariosService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  readonly cargandoRoles = signal(false);
  readonly guardando = signal(false);
  readonly roles = signal<ItemRol[]>([]);
  readonly rolesSeleccionados = signal<Set<string>>(new Set());
  readonly errorGeneral = signal<string | null>(null);
  readonly sinRolesError = signal(false);
  readonly mostrarContrasena = signal(false);

  readonly form = this.fb.nonNullable.group({
    nombreUsuario: ['', [Validators.required, Validators.maxLength(50)]],
    nombres: ['', Validators.required],
    apellidoPaterno: ['', Validators.required],
    apellidoMaterno: [''],
    correo: ['', Validators.email],
    contrasena: ['', [Validators.required, Validators.minLength(8)]],
  });

  ngOnInit(): void {
    this.cargarRoles();
  }

  private cargarRoles(): void {
    this.cargandoRoles.set(true);
    this.service.listarRoles().subscribe({
      next: (roles) => {
        this.roles.set(roles);
        this.cargandoRoles.set(false);
      },
      error: () => this.cargandoRoles.set(false),
    });
  }

  toggleRol(codigo: string, checked: boolean): void {
    const set = new Set(this.rolesSeleccionados());
    if (checked) {
      set.add(codigo);
    } else {
      set.delete(codigo);
    }
    this.rolesSeleccionados.set(set);
    this.sinRolesError.set(false);
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.rolesSeleccionados().size === 0) {
      this.sinRolesError.set(true);
      return;
    }

    this.guardando.set(true);
    this.errorGeneral.set(null);
    const v = this.form.getRawValue();

    this.service.crear({
      nombreUsuario: v.nombreUsuario,
      nombres: v.nombres,
      apellidoPaterno: v.apellidoPaterno,
      apellidoMaterno: v.apellidoMaterno || null,
      correo: v.correo || null,
      contrasena: v.contrasena,
      rolesIniciales: Array.from(this.rolesSeleccionados()),
    }).subscribe({
      next: (resp) => {
        this.router.navigate(['/usuarios', resp.id]);
      },
      error: (err: HttpErrorResponse) => {
        this.guardando.set(false);
        const code = err.error?.code;
        if (code === 'NOMBRE_USUARIO_DUPLICADO') {
          this.errorGeneral.set('El nombre de usuario ya está en uso.');
        } else if (code === 'CORREO_DUPLICADO') {
          this.errorGeneral.set('El correo electrónico ya está registrado.');
        } else {
          this.errorGeneral.set('Error al crear el usuario. Intente nuevamente.');
        }
      },
    });
  }
}
