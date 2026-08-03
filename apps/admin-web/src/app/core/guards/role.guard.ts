import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, map, take } from 'rxjs';
import { AuthService } from '../auth/auth.service';

/**
 * Protege rutas que requieren uno o más roles específicos.
 * Uso: canActivate: [roleGuard], data: { roles: ['ADMIN', 'SUPERVISOR'] }
 * Espera a que el bootstrap resuelva antes de decidir.
 */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requiredRoles: string[] = route.data['roles'] ?? [];

  return toObservable(authService.sessionState).pipe(
    filter((state) => state !== 'INICIALIZANDO'),
    take(1),
    map((state) => {
      if (state !== 'AUTENTICADA') return router.createUrlTree(['/login']);
      if (requiredRoles.length === 0) return true;
      const profile = authService.profile();
      const hasRole = profile?.roles?.some((r) => requiredRoles.includes(r)) ?? false;
      return hasRole ? true : router.createUrlTree(['/forbidden']);
    }),
  );
};
