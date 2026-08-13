export interface ItemEjecutivoAdmin {
  usuarioId: string;
  nombreUsuario: string;
  nombreCompleto: string;
  activo: boolean;
  codigoEjecutivoOrigen: string | null;
  supervisorId: string | null;
  supervisorNombre: string | null;
}

export interface ItemSupervisorAdmin {
  usuarioId: string;
  nombreUsuario: string;
  nombreCompleto: string;
}

export interface SolicitudAsignarSupervisor {
  supervisorId: string;
}

export interface SolicitudActualizarCodigo {
  codigo: string | null;
}
