import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="login-container">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-card-title>Iniciar sesión</mat-card-title>
          <mat-card-subtitle>Panel Administrativo</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Nombre de usuario</mat-label>
              <input
                matInput
                formControlName="nombreUsuario"
                autocomplete="username"
                data-testid="username-input"
              />
              @if (form.controls.nombreUsuario.hasError('required') && form.controls.nombreUsuario.touched) {
                <mat-error>El nombre de usuario es obligatorio</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Contraseña</mat-label>
              <input
                matInput
                [type]="showPassword() ? 'text' : 'password'"
                formControlName="clave"
                autocomplete="current-password"
                data-testid="password-input"
              />
              <button
                type="button"
                mat-icon-button
                matSuffix
                (click)="showPassword.set(!showPassword())"
              >
                <mat-icon>{{ showPassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (form.controls.clave.hasError('required') && form.controls.clave.touched) {
                <mat-error>La contraseña es obligatoria</mat-error>
              }
            </mat-form-field>

            @if (errorMessage()) {
              <p class="error-message" role="alert" data-testid="error-message">
                {{ errorMessage() }}
              </p>
            }

            <button
              mat-flat-button
              color="primary"
              type="submit"
              class="full-width"
              [disabled]="loading() || form.invalid"
              data-testid="submit-button"
            >
              @if (loading()) {
                <mat-spinner diameter="20" />
              } @else {
                Ingresar
              }
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: `
    .login-container {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100vh;
      background-color: #f5f5f5;
    }
    .login-card { width: 360px; padding: 16px; }
    .full-width { width: 100%; margin-bottom: 12px; }
    .error-message { color: var(--mat-error-color, #f44336); font-size: 13px; margin: 0 0 12px; }
  `,
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    nombreUsuario: ['', Validators.required],
    clave: ['', Validators.required],
  });

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly showPassword = signal(false);

  submit(): void {
    if (this.form.invalid || this.loading()) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    const { nombreUsuario, clave } = this.form.getRawValue();

    this.authService.login({ nombreUsuario, clave }).subscribe({
      next: () => {
        this.authService.loadProfile().subscribe({
          next: () => this.router.navigate(['/home']),
          error: () => this.router.navigate(['/home']),
        });
      },
      error: (err: unknown) => {
        this.loading.set(false);
        if (err instanceof HttpErrorResponse && err.status === 401) {
          this.errorMessage.set('Usuario o contraseña incorrectos.');
        } else {
          this.errorMessage.set('Error al conectar con el servidor. Intente nuevamente.');
        }
      },
    });
  }
}
