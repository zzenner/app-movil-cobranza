import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { TokenStorageService } from '../auth/token-storage.service';
import { AuthService } from '../auth/auth.service';

/** Rutas de autenticación que no deben llevar Bearer ni reintentar refresh */
const AUTH_PATHS = ['/api/v1/auth/web/login', '/api/v1/auth/web/refresh'];

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
) => {
  const tokenStorage = inject(TokenStorageService);
  const authService = inject(AuthService);

  if (AUTH_PATHS.some((p) => req.url.includes(p))) {
    return next(req);
  }

  const token = tokenStorage.getAccessToken();
  const authed = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authed).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && err.status === 401 && token) {
        return authService.refresh().pipe(
          switchMap((resp) => {
            const retried = req.clone({
              setHeaders: { Authorization: `Bearer ${resp.accessToken}` },
            });
            return next(retried);
          }),
          catchError((refreshErr) => throwError(() => refreshErr)),
        );
      }
      return throwError(() => err);
    }),
  );
};
