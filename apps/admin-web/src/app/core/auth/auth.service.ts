import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, throwError, shareReplay } from 'rxjs';
import { TokenStorageService } from './token-storage.service';
import { LoginRequest, LoginResponse, UserProfile } from './auth.models';

export type SessionState = 'INICIALIZANDO' | 'AUTENTICADA' | 'NO_AUTENTICADA';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);

  private readonly _sessionState = signal<SessionState>('INICIALIZANDO');
  private readonly _profile = signal<UserProfile | null>(null);

  readonly sessionState = this._sessionState.asReadonly();
  readonly profile = this._profile.asReadonly();
  readonly isAuthenticated = computed(() => this._sessionState() === 'AUTENTICADA');
  readonly isInitializing = computed(() => this._sessionState() === 'INICIALIZANDO');

  /** Shared single-flight refresh — múltiples llamadas concurrentes reciben la misma respuesta. */
  private refreshInFlight: Observable<LoginResponse> | null = null;

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/v1/auth/web/login', request).pipe(
      tap((resp) => {
        this.tokenStorage.setAccessToken(resp.accessToken);
        this._sessionState.set('AUTENTICADA');
      }),
    );
  }

  /**
   * Renueva el access token usando el refresh token de la cookie HttpOnly.
   * Garantiza que solo una petición de refresh se ejecute en paralelo (single-flight).
   */
  refresh(): Observable<LoginResponse> {
    if (!this.refreshInFlight) {
      this.refreshInFlight = this.http.post<LoginResponse>('/api/v1/auth/web/refresh', null).pipe(
        tap((resp) => {
          this.tokenStorage.setAccessToken(resp.accessToken);
          this._sessionState.set('AUTENTICADA');
        }),
        catchError((err) => {
          this.tokenStorage.clear();
          this._sessionState.set('NO_AUTENTICADA');
          this._profile.set(null);
          this.refreshInFlight = null;
          return throwError(() => err);
        }),
        shareReplay(1),
        tap({ complete: () => (this.refreshInFlight = null) }),
      );
    }
    return this.refreshInFlight;
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/v1/auth/web/logout', null).pipe(
      tap({
        next: () => this.limpiarEstado(),
        error: () => this.limpiarEstado(),
      }),
    );
  }

  loadProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>('/api/v1/auth/me').pipe(
      tap((profile) => this._profile.set(profile)),
    );
  }

  markAsAuthenticated(profile: UserProfile, accessToken: string): void {
    this.tokenStorage.setAccessToken(accessToken);
    this._profile.set(profile);
    this._sessionState.set('AUTENTICADA');
  }

  markAsUnauthenticated(): void {
    this.limpiarEstado();
  }

  private limpiarEstado(): void {
    this.tokenStorage.clear();
    this._sessionState.set('NO_AUTENTICADA');
    this._profile.set(null);
    this.refreshInFlight = null;
  }
}
