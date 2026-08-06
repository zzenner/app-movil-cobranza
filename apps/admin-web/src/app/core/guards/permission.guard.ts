import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, map, take } from 'rxjs';
import { AuthService } from '../auth/auth.service';

/**
 * Protege rutas que requieren un permiso específico.
 * Uso: canActivate: [permissionGuard], data: { permission: 'USUARIOS_VER' }
 * Espera a que el bootstrap resuelva antes de decidir.
 */
export const permissionGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requiredPermission: string | undefined = route.data['permission'];

  return toObservable(authService.sessionState).pipe(
    filter((state) => state !== 'INICIALIZANDO'),
    take(1),
    map((state) => {
      if (state !== 'AUTENTICADA') return router.createUrlTree(['/login']);
      if (!requiredPermission) return true;
      const profile = authService.profile();
      const hasPermission = profile?.permisos?.includes(requiredPermission) ?? false;
      return hasPermission ? true : router.createUrlTree(['/forbidden']);
    }),
  );
};
