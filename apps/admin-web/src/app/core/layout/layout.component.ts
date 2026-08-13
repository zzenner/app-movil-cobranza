import { Component, inject, computed } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
  ],
  template: `
    <mat-sidenav-container class="layout-container">
      <mat-sidenav mode="side" opened class="sidenav">
        <mat-nav-list>
          <a mat-list-item routerLink="/home">
            <mat-icon matListItemIcon>home</mat-icon>
            <span matListItemTitle>Inicio</span>
          </a>
          @if (tienePermissoUsuariosVer()) {
            <a mat-list-item routerLink="/usuarios" data-testid="menu-usuarios">
              <mat-icon matListItemIcon>people</mat-icon>
              <span matListItemTitle>Usuarios</span>
            </a>
          }
          @if (tienePermisoImportacion()) {
            <a mat-list-item routerLink="/importacion" data-testid="menu-importacion">
              <mat-icon matListItemIcon>upload_file</mat-icon>
              <span matListItemTitle>Importaciones</span>
            </a>
          }
          @if (tienePermisoCarteras()) {
            <a mat-list-item routerLink="/carteras" data-testid="menu-carteras">
              <mat-icon matListItemIcon>folder_special</mat-icon>
              <span matListItemTitle>Carteras</span>
            </a>
          }
          @if (tienePermisoSupervision()) {
            <a mat-list-item routerLink="/supervision" data-testid="menu-supervision">
              <mat-icon matListItemIcon>supervisor_account</mat-icon>
              <span matListItemTitle>Supervisión</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content class="content">
        <mat-toolbar color="primary">
          <span>Panel Administrativo</span>
          <span class="spacer"></span>
          @if (profile()) {
            <span class="usuario">{{ profile()!.nombreUsuario }}</span>
          }
          <button mat-icon-button (click)="logout()" title="Cerrar sesión">
            <mat-icon>logout</mat-icon>
          </button>
        </mat-toolbar>

        <div class="page-content">
          <router-outlet />
        </div>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: `
    .layout-container { height: 100vh; }
    .sidenav { width: 220px; }
    .content { display: flex; flex-direction: column; }
    .spacer { flex: 1; }
    .usuario { margin-right: 8px; font-size: 14px; }
    .page-content { padding: 24px; }
  `,
})
export class LayoutComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly profile = this.authService.profile;
  readonly tienePermissoUsuariosVer = computed(() =>
    this.authService.profile()?.permisos?.includes('USUARIOS_VER') ?? false,
  );
  readonly tienePermisoImportacion = computed(() =>
    this.authService.profile()?.permisos?.includes('DATOS_IMPORTAR') ?? false,
  );
  readonly tienePermisoCarteras = computed(() =>
    this.authService.profile()?.permisos?.includes('CARTERAS_VER') ?? false,
  );
  readonly tienePermisoSupervision = computed(() =>
    this.authService.profile()?.permisos?.includes('SUPERVISION_VER') ?? false,
  );

  logout(): void {
    this.authService.logout().subscribe({
      complete: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login']),
    });
  }
}
