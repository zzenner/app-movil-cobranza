import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ItemCatalogoCartera } from '../models/cartera.models';

@Injectable({ providedIn: 'root' })
export class CarterasService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/admin/carteras';

  listarCatalogo(): Observable<ItemCatalogoCartera[]> {
    return this.http.get<ItemCatalogoCartera[]>(this.base);
  }
}
