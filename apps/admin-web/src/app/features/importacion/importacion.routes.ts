import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/guards/permission.guard';
import { authGuard } from '../../core/guards/auth.guard';

export const importacionRoutes: Routes = [
  {
    path: '',
    canActivate: [authGuard, permissionGuard],
    data: { permission: 'DATOS_IMPORTAR' },
    loadComponent: () =>
      import('./components/importacion-list/importacion-list.component').then(
        (m) => m.ImportacionListComponent,
      ),
  },
  {
    path: 'nueva',
    canActivate: [authGuard, permissionGuard],
    data: { permission: 'DATOS_IMPORTAR' },
    loadComponent: () =>
      import('./components/importacion-nueva/importacion-nueva.component').then(
        (m) => m.ImportacionNuevaComponent,
      ),
  },
  {
    path: ':id',
    canActivate: [authGuard, permissionGuard],
    data: { permission: 'DATOS_IMPORTAR' },
    loadComponent: () =>
      import('./components/importacion-detail/importacion-detail.component').then(
        (m) => m.ImportacionDetailComponent,
      ),
  },
];
