import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, map, take } from 'rxjs';
import { AuthService } from '../auth/auth.service';

/** Redirige a /home si el usuario ya está autenticado (para no volver a mostrar el login). */
export const loginGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return toObservable(authService.sessionState).pipe(
    filter((state) => state !== 'INICIALIZANDO'),
    take(1),
    map((state) => {
      if (state === 'AUTENTICADA') return router.createUrlTree(['/home']);
      return true;
    }),
  );
};
