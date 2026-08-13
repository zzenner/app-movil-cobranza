import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/guards/permission.guard';

export const asignacionesRoutes: Routes = [
  {
    path: '',
    canActivate: [permissionGuard],
    data: { permission: 'ASIGNACIONES_VER' },
    loadComponent: () =>
      import('./components/asignaciones-list/asignaciones-list.component').then(
        (m) => m.AsignacionesListComponent,
      ),
  },
  {
    path: 'crear',
    canActivate: [permissionGuard],
    data: { permission: 'ASIGNACIONES_ADMINISTRAR' },
    loadComponent: () =>
      import('./components/asignacion-create/asignacion-create.component').then(
        (m) => m.AsignacionCreateComponent,
      ),
  },
  {
    path: ':id',
    canActivate: [permissionGuard],
    data: { permission: 'ASIGNACIONES_VER' },
    loadComponent: () =>
      import('./components/asignacion-detail/asignacion-detail.component').then(
        (m) => m.AsignacionDetailComponent,
      ),
  },
];
