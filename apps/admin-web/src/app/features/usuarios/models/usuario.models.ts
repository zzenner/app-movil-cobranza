export type EstadoUsuario = 'ACTIVO' | 'BLOQUEADO_TEMPORAL' | 'BLOQUEADO' | 'INACTIVO';

export interface RolVigente {
  codigo: string;
  fechaAsignacion: string;
}

export interface ItemListadoUsuario {
  id: string;
  nombreUsuario: string;
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno: string | null;
  correo: string | null;
  estadoCalculado: EstadoUsuario;
  bloqueadoHasta: string | null;
  roles: string[];
  supervisorId: string | null;
  supervisorNombreUsuario: string | null;
  fechaCreacion: string;
}

export interface RespuestaListadoUsuarios {
  contenido: ItemListadoUsuario[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface DetalleUsuario {
  id: string;
  nombreUsuario: string;
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno: string | null;
  correo: string | null;
  estadoCalculado: EstadoUsuario;
  activo: boolean;
  bloqueado: boolean;
  bloqueadoHasta: string | null;
  intentosFallidos: number;
  fechaUltimoAcceso: string | null;
  roles: RolVigente[];
  permisosEfectivos: string[];
  supervisorId: string | null;
  supervisorNombreUsuario: string | null;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface FiltrosListado {
  nombreUsuario?: string;
  estado?: string;
  rol?: string;
}
