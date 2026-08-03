import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, map, take } from 'rxjs';
import { AuthService } from '../auth/auth.service';

/**
 * Espera a que la sesión se resuelva (INICIALIZANDO → *) antes de decidir.
 * Redirige a /login si el usuario no está autenticado.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return toObservable(authService.sessionState).pipe(
    filter((state) => state !== 'INICIALIZANDO'),
    take(1),
    map((state) => {
      if (state === 'AUTENTICADA') return true;
      return router.createUrlTree(['/login']);
    }),
  );
};
