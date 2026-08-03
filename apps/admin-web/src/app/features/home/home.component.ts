import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [MatCardModule, MatChipsModule, MatIconModule],
  template: `
    <h2>Inicio</h2>

    @if (profile(); as p) {
      <mat-card>
        <mat-card-header>
          <mat-icon mat-card-avatar>account_circle</mat-icon>
          <mat-card-title>{{ p.nombreUsuario }}</mat-card-title>
          <mat-card-subtitle>Sesión activa</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <p><strong>ID de sesión:</strong> {{ p.sesionId }}</p>
          <p><strong>Tipo de cliente:</strong> {{ p.tipoCliente }}</p>

          @if (p.roles.length > 0) {
            <p><strong>Roles:</strong></p>
            <mat-chip-set>
              @for (rol of p.roles; track rol) {
                <mat-chip>{{ rol }}</mat-chip>
              }
            </mat-chip-set>
          }

          @if (p.permisos.length > 0) {
            <p><strong>Permisos:</strong></p>
            <mat-chip-set>
              @for (permiso of p.permisos; track permiso) {
                <mat-chip>{{ permiso }}</mat-chip>
              }
            </mat-chip-set>
          }
        </mat-card-content>
      </mat-card>
    } @else {
      <p>Cargando perfil...</p>
    }
  `,
  styles: `
    h2 { margin: 0 0 24px; }
    mat-card { max-width: 600px; }
    mat-card-content p { margin: 8px 0; }
    mat-chip-set { margin-bottom: 8px; }
  `,
})
export class HomeComponent {
  readonly profile = inject(AuthService).profile;
}
