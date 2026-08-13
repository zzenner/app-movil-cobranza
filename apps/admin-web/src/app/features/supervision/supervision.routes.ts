import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { permissionGuard } from '../../core/guards/permission.guard';

export const supervisionRoutes: Routes = [
  {
    path: '',
    canActivate: [authGuard, permissionGuard],
    data: { permission: 'SUPERVISION_VER' },
    loadComponent: () =>
      import('./components/supervision-list/supervision-list.component').then(
        (m) => m.SupervisionListComponent,
      ),
  },
];
