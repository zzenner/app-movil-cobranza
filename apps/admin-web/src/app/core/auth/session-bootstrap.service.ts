import { Injectable, inject } from '@angular/core';
import { Observable, switchMap, catchError, of } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Ejecutado una sola vez en el arranque de la app (APP_INITIALIZER).
 * Intenta renovar la sesión desde la cookie de refresh token.
 * Si falla, marca el estado como NO_AUTENTICADA.
 * Los guards esperan a que INICIALIZANDO se resuelva antes de decidir.
 */
@Injectable({ providedIn: 'root' })
export class SessionBootstrapService {
  private readonly authService = inject(AuthService);

  bootstrap(): Observable<void> {
    return this.authService.refresh().pipe(
      switchMap(() => this.authService.loadProfile()),
      catchError(() => {
        this.authService.markAsUnauthenticated();
        return of(undefined);
      }),
    ) as Observable<void>;
  }
}
