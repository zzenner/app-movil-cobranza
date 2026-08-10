export type EstadoImportacion =
  | 'RECIBIDA'
  | 'VALIDANDO'
  | 'VALIDADA'
  | 'CON_ERRORES'
  | 'PROCESANDO'
  | 'COMPLETADA'
  | 'FALLIDA'
  | 'EXPIRADA';

export interface ImportacionResumen {
  id: string;
  carteraId: string;
  periodo: string;
  sistemaOrigen: string;
  estado: EstadoImportacion;
  nombreArchivoOriginal: string;
  filasTotales: number | null;
  filasProcesadas: number | null;
  filasRechazadas: number | null;
  filasAdvertencia: number | null;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface ImportacionDetalle extends ImportacionResumen {
  usuarioId: string;
  personasCreadas: number | null;
  personasActualizadas: number | null;
  operacionesCreadas: number | null;
  operacionesActualizadas: number | null;
  cuotasCreadas: number | null;
  cuotasActualizadas: number | null;
  mensajeError: string | null;
  version: number;
}

export interface ErrorImportacion {
  id: string;
  numeroFila: number | null;
  columna: string | null;
  codigoError: string;
  nivel: 'ERROR' | 'ADVERTENCIA';
  mensaje: string;
}

export interface RespuestaPaginaImportaciones {
  contenido: ImportacionResumen[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface RespuestaPaginaErrores {
  contenido: ErrorImportacion[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface RespuestaCrearImportacion {
  importacionId: string;
  estado: string;
  periodo: string;
  nombreArchivoOriginal: string;
}

export interface ItemCartera {
  id: string;
  nombre: string;
}

export type FiltrosListado = {
  carteraId?: string;
  pagina?: number;
  tamanio?: number;
};

export const ESTADOS_EN_PROGRESO: EstadoImportacion[] = ['RECIBIDA', 'VALIDANDO', 'PROCESANDO'];
export const ESTADOS_TERMINALES: EstadoImportacion[] = ['COMPLETADA', 'FALLIDA', 'EXPIRADA'];
