import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DetalleUsuario, FiltrosListado, RespuestaListadoUsuarios } from '../models/usuario.models';

@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/admin/usuarios';

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
}
