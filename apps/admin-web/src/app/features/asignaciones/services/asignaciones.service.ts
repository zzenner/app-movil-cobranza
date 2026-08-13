import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DetalleAsignacionDiariaAdmin,
  ItemAsignacionDiariaAdmin,
  ItemAsignacionMensualAdmin,
  ItemPeriodo,
  ItemPersonaDisponible,
  RespuestaCreacion,
  SolicitudActualizarPersonas,
  SolicitudCancelar,
  SolicitudCrearBorrador,
} from '../models/asignacion.models';

@Injectable({ providedIn: 'root' })
export class AsignacionesService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/admin/asignaciones';

  listarPeriodos(filtros?: { carteraId?: string; supervisorId?: string; ejecutivoId?: string }): Observable<ItemPeriodo[]> {
    let params = new HttpParams();
    if (filtros?.carteraId) params = params.set('carteraId', filtros.carteraId);
    if (filtros?.supervisorId) params = params.set('supervisorId', filtros.supervisorId);
    if (filtros?.ejecutivoId) params = params.set('ejecutivoId', filtros.ejecutivoId);
    return this.http.get<ItemPeriodo[]>(`${this.base}/periodos`, { params });
  }

  listarMensuales(filtros?: {
    periodo?: string;
    carteraId?: string;
    supervisorId?: string;
    ejecutivoId?: string;
  }): Observable<ItemAsignacionMensualAdmin[]> {
    let params = new HttpParams();
    if (filtros?.periodo) params = params.set('periodo', filtros.periodo);
    if (filtros?.carteraId) params = params.set('carteraId', filtros.carteraId);
    if (filtros?.supervisorId) params = params.set('supervisorId', filtros.supervisorId);
    if (filtros?.ejecutivoId) params = params.set('ejecutivoId', filtros.ejecutivoId);
    return this.http.get<ItemAsignacionMensualAdmin[]>(`${this.base}/mensuales`, { params });
  }

  listarPersonasDisponibles(mensualId: string): Observable<ItemPersonaDisponible[]> {
    return this.http.get<ItemPersonaDisponible[]>(`${this.base}/mensuales/${mensualId}/personas-disponibles`);
  }

  listarDiarias(filtros?: {
    fecha?: string;
    estado?: string;
    carteraId?: string;
    supervisorId?: string;
    ejecutivoId?: string;
  }): Observable<ItemAsignacionDiariaAdmin[]> {
    let params = new HttpParams();
    if (filtros?.fecha) params = params.set('fecha', filtros.fecha);
    if (filtros?.estado) params = params.set('estado', filtros.estado);
    if (filtros?.carteraId) params = params.set('carteraId', filtros.carteraId);
    if (filtros?.supervisorId) params = params.set('supervisorId', filtros.supervisorId);
    if (filtros?.ejecutivoId) params = params.set('ejecutivoId', filtros.ejecutivoId);
    return this.http.get<ItemAsignacionDiariaAdmin[]>(`${this.base}/diarias`, { params });
  }

  obtenerDetalle(id: string): Observable<DetalleAsignacionDiariaAdmin> {
    return this.http.get<DetalleAsignacionDiariaAdmin>(`${this.base}/diarias/${id}`);
  }

  crearBorrador(solicitud: SolicitudCrearBorrador): Observable<RespuestaCreacion> {
    return this.http.post<RespuestaCreacion>(`${this.base}/diarias`, solicitud);
  }

  actualizarPersonas(id: string, solicitud: SolicitudActualizarPersonas): Observable<void> {
    return this.http.put<void>(`${this.base}/diarias/${id}/personas`, solicitud);
  }

  publicar(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/diarias/${id}/publicar`, {});
  }

  cancelar(id: string, solicitud: SolicitudCancelar): Observable<void> {
    return this.http.post<void>(`${this.base}/diarias/${id}/cancelar`, solicitud);
  }
}
