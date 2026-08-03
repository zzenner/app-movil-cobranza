import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterLink, MatButtonModule],
  template: `
    <div class="container">
      <h1>404 — Página no encontrada</h1>
      <p>La dirección solicitada no existe.</p>
      <a mat-button routerLink="/home">Volver al inicio</a>
    </div>
  `,
  styles: `.container { text-align: center; padding: 64px 24px; }`,
})
export class NotFoundComponent {}
