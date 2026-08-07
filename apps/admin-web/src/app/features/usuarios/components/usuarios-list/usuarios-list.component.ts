import {
  Component, OnInit, OnDestroy, inject, signal, computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { UsuariosService } from '../../services/usuarios.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { ItemListadoUsuario, RespuestaListadoUsuarios } from '../../models/usuario.models';

@Component({
  selector: 'app-usuarios-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatIconModule,
  ],
  template: `
    <div class="page-header">
      <h1>Usuarios</h1>
      @if (puedeAdministrar()) {
        <a mat-raised-button color="primary" routerLink="/usuarios/nuevo" data-testid="btn-nuevo-usuario">
          <mat-icon>add</mat-icon> Nuevo usuario
        </a>
      }
    </div>

    <div class="filtros" [formGroup]="filtrosForm">
      <mat-form-field appearance="outline">
        <mat-label>Usuario</mat-label>
        <input matInput formControlName="nombreUsuario" placeholder="Buscar por nombre de usuario" />
        <mat-icon matSuffix>search</mat-icon>
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Estado</mat-label>
        <mat-select formControlName="estado">
          <mat-option value="">Todos</mat-option>
          <mat-option value="ACTIVO">Activo</mat-option>
          <mat-option value="BLOQUEADO_TEMPORAL">Bloqueado temporal</mat-option>
          <mat-option value="BLOQUEADO">Bloqueado</mat-option>
          <mat-option value="INACTIVO">Inactivo</mat-option>
        </mat-select>
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Rol</mat-label>
        <mat-select formControlName="rol">
          <mat-option value="">Todos</mat-option>
          <mat-option value="JEFE_SUPERVISORES">Jefe de Supervisores</mat-option>
          <mat-option value="TECNOLOGIA">Tecnología</mat-option>
          <mat-option value="SUPERVISOR">Supervisor</mat-option>
          <mat-option value="EJECUTIVO_TERRENO">Ejecutivo de Terreno</mat-option>
        </mat-select>
      </mat-form-field>
    </div>

    @if (cargando()) {
      <div class="loading-container">
        <mat-spinner diameter="40" />
      </div>
    } @else if (error()) {
      <div class="error-container" data-testid="error-listado">
        <mat-icon color="warn">error</mat-icon>
        <p>Error al cargar usuarios. <button mat-button (click)="cargar()">Reintentar</button></p>
      </div>
    } @else if (totalElementos() === 0) {
      <div class="empty-container" data-testid="empty-listado">
        <mat-icon>people_outline</mat-icon>
        <p>No se encontraron usuarios con los filtros actuales.</p>
      </div>
    } @else {
      <table mat-table [dataSource]="usuarios()" class="usuarios-table">
        <ng-container matColumnDef="nombreUsuario">
          <th mat-header-cell *matHeaderCellDef>Usuario</th>
          <td mat-cell *matCellDef="let u">{{ u.nombreUsuario }}</td>
        </ng-container>

        <ng-container matColumnDef="nombreCompleto">
          <th mat-header-cell *matHeaderCellDef>Nombre completo</th>
          <td mat-cell *matCellDef="let u">
            {{ u.nombres }} {{ u.apellidoPaterno }}
            {{ u.apellidoMaterno ? u.apellidoMaterno : '' }}
          </td>
        </ng-container>

        <ng-container matColumnDef="roles">
          <th mat-header-cell *matHeaderCellDef>Roles</th>
          <td mat-cell *matCellDef="let u">
            @for (rol of u.roles; track rol) {
              <span class="chip-rol">{{ rol }}</span>
            }
          </td>
        </ng-container>

        <ng-container matColumnDef="estado">
          <th mat-header-cell *matHeaderCellDef>Estado</th>
          <td mat-cell *matCellDef="let u">
            <span [class]="'estado-badge estado-' + u.estadoCalculado.toLowerCase()">
              {{ u.estadoCalculado }}
            </span>
          </td>
        </ng-container>

        <ng-container matColumnDef="supervisor">
          <th mat-header-cell *matHeaderCellDef>Supervisor</th>
          <td mat-cell *matCellDef="let u">
            {{ u.supervisorNombreUsuario ?? '—' }}
          </td>
        </ng-container>

        <ng-container matColumnDef="fechaCreacion">
          <th mat-header-cell *matHeaderCellDef>Creado</th>
          <td mat-cell *matCellDef="let u">
            {{ u.fechaCreacion | date:'dd/MM/yyyy' }}
          </td>
        </ng-container>

        <ng-container matColumnDef="acciones">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let u">
            <a mat-button color="primary"
               [routerLink]="['/usuarios', u.id]"
               [queryParams]="queryParamsActuales()">
              Ver detalle
            </a>
          </td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="columnas"></tr>
        <tr mat-row *matRowDef="let row; columns: columnas;"></tr>
      </table>

      <mat-paginator
        [length]="totalElementos()"
        [pageSize]="tamanio"
        [pageIndex]="pagina()"
        [pageSizeOptions]="[10, 20, 50]"
        (page)="onPaginar($event)"
        showFirstLastButtons />
    }
  `,
  styles: `
    .page-header { margin-bottom: 24px; }
    .filtros {
      display: flex;
      gap: 16px;
      flex-wrap: wrap;
      margin-bottom: 16px;
    }
    .filtros mat-form-field { min-width: 200px; }
    .loading-container, .error-container, .empty-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px;
      gap: 16px;
      color: rgba(0,0,0,.54);
    }
    .usuarios-table { width: 100%; }
    .chip-rol {
      display: inline-block;
      background: #e3f2fd;
      border-radius: 4px;
      padding: 2px 6px;
      font-size: 12px;
      margin-right: 4px;
    }
    .estado-badge {
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }
    .estado-activo { background: #e8f5e9; color: #2e7d32; }
    .estado-bloqueado_temporal { background: #fff8e1; color: #f57f17; }
    .estado-bloqueado { background: #ffebee; color: #c62828; }
    .estado-inactivo { background: #eceff1; color: #546e7a; }
  `,
})
export class UsuariosListComponent implements OnInit, OnDestroy {
  private readonly service = inject(UsuariosService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);

  get puedeAdministrar(): () => boolean {
    return () => this.auth.profile()?.permisos?.includes('USUARIOS_ADMINISTRAR') ?? false;
  }
  private readonly destroy$ = new Subject<void>();

  readonly columnas = ['nombreUsuario', 'nombreCompleto', 'roles', 'estado', 'supervisor', 'fechaCreacion', 'acciones'];
  readonly tamanio = 20;

  readonly cargando = signal(false);
  readonly error = signal(false);
  readonly usuarios = signal<ItemListadoUsuario[]>([]);
  readonly totalElementos = signal(0);
  readonly pagina = signal(0);

  readonly filtrosForm = new FormGroup({
    nombreUsuario: new FormControl(''),
    estado: new FormControl(''),
    rol: new FormControl(''),
  });

  readonly queryParamsActuales = computed(() => ({
    pagina: this.pagina(),
    nombreUsuario: this.filtrosForm.value.nombreUsuario || undefined,
    estado: this.filtrosForm.value.estado || undefined,
    rol: this.filtrosForm.value.rol || undefined,
  }));

  ngOnInit(): void {
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const p = parseInt(params['pagina'] ?? '0', 10);
      this.pagina.set(isNaN(p) ? 0 : p);
      this.filtrosForm.setValue({
        nombreUsuario: params['nombreUsuario'] ?? '',
        estado: params['estado'] ?? '',
        rol: params['rol'] ?? '',
      }, { emitEvent: false });
      this.cargar();
    });

    this.filtrosForm.get('nombreUsuario')!.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(() => this.reiniciarPaginaYCargar());

    this.filtrosForm.get('estado')!.valueChanges.pipe(
      takeUntil(this.destroy$),
    ).subscribe(() => this.reiniciarPaginaYCargar());

    this.filtrosForm.get('rol')!.valueChanges.pipe(
      takeUntil(this.destroy$),
    ).subscribe(() => this.reiniciarPaginaYCargar());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cargar(): void {
    const v = this.filtrosForm.value;
    this.cargando.set(true);
    this.error.set(false);
    this.service.listar(this.pagina(), this.tamanio, {
      nombreUsuario: v.nombreUsuario ?? undefined,
      estado: v.estado || undefined,
      rol: v.rol || undefined,
    }).subscribe({
      next: (resp) => {
        this.usuarios.set(resp.contenido);
        this.totalElementos.set(resp.totalElementos);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set(true);
      },
    });
  }

  onPaginar(event: PageEvent): void {
    const v = this.filtrosForm.value;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        pagina: event.pageIndex,
        nombreUsuario: v.nombreUsuario || undefined,
        estado: v.estado || undefined,
        rol: v.rol || undefined,
      },
      queryParamsHandling: 'merge',
    });
  }

  private reiniciarPaginaYCargar(): void {
    const v = this.filtrosForm.value;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        pagina: 0,
        nombreUsuario: v.nombreUsuario || undefined,
        estado: v.estado || undefined,
        rol: v.rol || undefined,
      },
    });
  }
}
