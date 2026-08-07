import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DetalleUsuario,
  FiltrosListado,
  ItemRol,
  RespuestaCrearUsuario,
  RespuestaListadoUsuarios,
  SolicitudActualizarDatosBasicosUsuario,
  SolicitudCrearUsuario,
  SolicitudRestablecerContrasena,
} from '../models/usuario.models';

@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/admin/usuarios';
  private readonly baseRoles = '/api/v1/admin/roles';

  listar(pagina: number, tamanio: number, filtros: FiltrosListado): Observable<RespuestaListadoUsuarios> {
    let params = new HttpParams()
      .set('pagina', pagina.toString())
      .set('tamanio', tamanio.toString());

    if (filtros.nombreUsuario?.trim()) {
      params = params.set('nombreUsuario', filtros.nombreUsuario.trim());
    }
    if (filtros.estado) {
      params = params.set('estado', filtros.estado);
    }
    if (filtros.rol) {
      params = params.set('rol', filtros.rol);
    }

    return this.http.get<RespuestaListadoUsuarios>(this.base, { params });
  }

  obtenerDetalle(id: string): Observable<DetalleUsuario> {
    return this.http.get<DetalleUsuario>(`${this.base}/${id}`);
  }

  listarRoles(): Observable<ItemRol[]> {
    return this.http.get<ItemRol[]>(this.baseRoles);
  }

  crear(solicitud: SolicitudCrearUsuario): Observable<RespuestaCrearUsuario> {
    return this.http.post<RespuestaCrearUsuario>(this.base, solicitud);
  }

  actualizarDatosBasicos(id: string, solicitud: SolicitudActualizarDatosBasicosUsuario): Observable<void> {
    return this.http.put<void>(`${this.base}/${id}/datos-basicos`, solicitud);
  }

  activar(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/activar`, null);
  }

  desactivar(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/desactivar`, null);
  }

  bloquear(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/bloquear`, null);
  }

  desbloquear(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/desbloquear`, null);
  }

  restablecerContrasena(id: string, solicitud: SolicitudRestablecerContrasena): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/restablecer-contrasena`, solicitud);
  }
}
