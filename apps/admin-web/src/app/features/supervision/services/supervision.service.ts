import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ItemEjecutivoAdmin,
  ItemSupervisorAdmin,
  SolicitudActualizarCodigo,
  SolicitudAsignarSupervisor,
} from '../models/supervision.models';

@Injectable({ providedIn: 'root' })
export class SupervisionService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/admin/supervision';

  listarEjecutivos(filtros?: {
    nombreUsuario?: string;
    codigo?: string;
    supervisorId?: string;
    sinSupervisor?: boolean;
  }): Observable<ItemEjecutivoAdmin[]> {
    let params = new HttpParams();
    if (filtros?.nombreUsuario?.trim()) params = params.set('nombreUsuario', filtros.nombreUsuario.trim());
    if (filtros?.codigo?.trim()) params = params.set('codigo', filtros.codigo.trim());
    if (filtros?.supervisorId) params = params.set('supervisorId', filtros.supervisorId);
    if (filtros?.sinSupervisor) params = params.set('sinSupervisor', 'true');
    return this.http.get<ItemEjecutivoAdmin[]>(`${this.base}/ejecutivos`, { params });
  }

  listarSupervisores(): Observable<ItemSupervisorAdmin[]> {
    return this.http.get<ItemSupervisorAdmin[]>(`${this.base}/supervisores`);
  }

  asignarOReaasignar(ejecutivoId: string, solicitud: SolicitudAsignarSupervisor): Observable<void> {
    return this.http.post<void>(`${this.base}/ejecutivos/${ejecutivoId}/supervisor`, solicitud);
  }

  quitarSupervision(ejecutivoId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/ejecutivos/${ejecutivoId}/supervisor`);
  }

  actualizarCodigo(ejecutivoId: string, solicitud: SolicitudActualizarCodigo): Observable<void> {
    return this.http.patch<void>(`${this.base}/ejecutivos/${ejecutivoId}/codigo`, solicitud);
  }
}
