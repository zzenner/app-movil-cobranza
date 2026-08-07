import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/guards/permission.guard';
import { authGuard } from '../../core/guards/auth.guard';

export const usuariosRoutes: Routes = [
  {
    path: '',
    canActivate: [authGuard, permissionGuard],
    data: { permission: 'USUARIOS_VER' },
    loadComponent: () =>
      import('./components/usuarios-list/usuarios-list.component').then(
        (m) => m.UsuariosListComponent,
      ),
  },
  {
    path: 'nuevo',
    canActivate: [authGuard, permissionGuard],
    data: { permission: 'USUARIOS_ADMINISTRAR' },
    loadComponent: () =>
      import('./components/usuario-create/usuario-create.component').then(
        (m) => m.UsuarioCreateComponent,
      ),
  },
  {
    path: ':id',
    canActivate: [authGuard, permissionGuard],
    data: { permission: 'USUARIOS_VER' },
    loadComponent: () =>
      import('./components/usuario-detail/usuario-detail.component').then(
        (m) => m.UsuarioDetailComponent,
      ),
  },
  {
    path: ':id/editar',
    canActivate: [authGuard, permissionGuard],
    data: { permission: 'USUARIOS_ADMINISTRAR' },
    loadComponent: () =>
      import('./components/usuario-edit/usuario-edit.component').then(
        (m) => m.UsuarioEditComponent,
      ),
  },
];
