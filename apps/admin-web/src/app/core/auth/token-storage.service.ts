import { Injectable } from '@angular/core';

/**
 * Almacena el access token únicamente en memoria de proceso.
 * No persiste en localStorage, sessionStorage ni cookies.
 * Al recargar la página, el estado se pierde y se renegocia vía refresh token (cookie HttpOnly).
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  private accessToken: string | null = null;

  setAccessToken(token: string): void {
    this.accessToken = token;
  }

  getAccessToken(): string | null {
    return this.accessToken;
  }

  clear(): void {
    this.accessToken = null;
  }
}
