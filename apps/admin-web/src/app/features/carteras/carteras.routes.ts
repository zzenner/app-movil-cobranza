import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { permissionGuard } from '../../core/guards/permission.guard';

export const carterasRoutes: Routes = [
  {
    path: '',
    canActivate: [authGuard, permissionGuard],
    data: { permission: 'CARTERAS_VER' },
    loadComponent: () =>
      import('./components/carteras-list/carteras-list.component').then(
        (m) => m.CarterasListComponent,
      ),
  },
];
