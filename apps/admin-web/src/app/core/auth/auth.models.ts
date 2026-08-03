export interface LoginRequest {
  nombreUsuario: string;
  clave: string;
}

export interface LoginResponse {
  accessToken: string;
  expiresInSeconds: number;
  sessionExpiresAt: string;
}

export interface UserProfile {
  usuarioId: string;
  sesionId: string;
  dispositivoId: string | null;
  tipoCliente: string;
  nombreUsuario: string;
  roles: string[];
  permisos: string[];
}
