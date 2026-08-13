import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ImportacionDetalle,
  ImportacionResumen,
  ItemCartera,
  RespuestaCrearImportacion,
  RespuestaPaginaErrores,
  RespuestaPaginaImportaciones,
} from '../models/importacion.models';

@Injectable({ providedIn: 'root' })
export class ImportacionService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/admin/importaciones/mensuales';
  private readonly baseCarteras = '/api/v1/admin/carteras';

  listarCarteras(): Observable<ItemCartera[]> {
    return this.http.get<ItemCartera[]>(`${this.baseCarteras}/activas`);
  }

  // Contrato v2: carteraId y periodo provienen del CSV, no del request
  crear(
    sistemaOrigen: string,
    archivo: File
  ): Observable<RespuestaCrearImportacion> {
    const formData = new FormData();
    formData.append('sistemaOrigen', sistemaOrigen);
    formData.append('archivo', archivo, archivo.name);
    return this.http.post<RespuestaCrearImportacion>(this.base, formData);
  }

  listar(pagina: number, tamanio: number, carteraId?: string): Observable<RespuestaPaginaImportaciones> {
    let params = new HttpParams()
      .set('pagina', pagina.toString())
      .set('tamanio', tamanio.toString());
    if (carteraId) params = params.set('carteraId', carteraId);
    return this.http.get<RespuestaPaginaImportaciones>(this.base, { params });
  }

  obtener(id: string): Observable<ImportacionDetalle> {
    return this.http.get<ImportacionDetalle>(`${this.base}/${id}`);
  }

  listarErrores(id: string, pagina: number, tamanio: number): Observable<RespuestaPaginaErrores> {
    const params = new HttpParams()
      .set('pagina', pagina.toString())
      .set('tamanio', tamanio.toString());
    return this.http.get<RespuestaPaginaErrores>(`${this.base}/${id}/errores`, { params });
  }

  confirmar(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/confirmar`, null);
  }
}
