export interface ItemPeriodo {
  periodo: string;
}

export interface ItemAsignacionMensualAdmin {
  id: string;
  periodo: string;
  carteraId: string;
  nombreCartera: string;
  ejecutivoId: string;
  nombreEjecutivo: string;
  codigoEjecutivo: string | null;
  supervisorId: string;
  nombreSupervisor: string;
  cantidadPersonas: number;
}

export interface ItemPersonaDisponible {
  personaId: string;
  rutNumero: string;
  rutDv: string;
  nombre: string;
  carteraId: string;
  nombreCartera: string;
  cantidadOperaciones: number;
  tieneAsignacionDiaria: boolean;
}

export interface ItemAsignacionDiariaAdmin {
  id: string;
  fecha: string;
  periodo: string;
  carteraId: string;
  nombreCartera: string;
  ejecutivoId: string;
  nombreEjecutivo: string;
  supervisorId: string;
  nombreSupervisor: string;
  estado: EstadoAsignacionDiaria;
  fechaPublicacion: string | null;
  cantidadPersonas: number;
}

export interface ItemPersonaEnDiaria {
  personaId: string;
  rutNumero: string;
  rutDv: string;
  nombre: string;
}

export interface DetalleAsignacionDiariaAdmin {
  id: string;
  fecha: string;
  periodo: string;
  carteraId: string;
  nombreCartera: string;
  ejecutivoId: string;
  nombreEjecutivo: string;
  supervisorId: string;
  nombreSupervisor: string;
  estado: EstadoAsignacionDiaria;
  fechaPublicacion: string | null;
  publicadoPorId: string | null;
  nombrePublicador: string | null;
  motivoCancelacion: string | null;
  fechaCreacion: string;
  version: number;
  cantidadPersonas: number;
  personas: ItemPersonaEnDiaria[];
}

export type EstadoAsignacionDiaria = 'BORRADOR' | 'PUBLICADA' | 'FINALIZADA' | 'CANCELADA';

export interface SolicitudCrearBorrador {
  asignacionMensualId: string;
  fecha: string;
  personaIds: string[];
}

export interface SolicitudActualizarPersonas {
  personaIds: string[];
}

export interface SolicitudCancelar {
  motivo: string;
}

export interface RespuestaCreacion {
  id: string;
}
