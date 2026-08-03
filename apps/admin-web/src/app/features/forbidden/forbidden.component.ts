import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-forbidden',
  standalone: true,
  imports: [RouterLink, MatButtonModule],
  template: `
    <div class="container">
      <h1>403 — Acceso denegado</h1>
      <p>No tiene permisos para acceder a esta sección.</p>
      <a mat-button routerLink="/home">Volver al inicio</a>
    </div>
  `,
  styles: `.container { text-align: center; padding: 64px 24px; }`,
})
export class ForbiddenComponent {}
