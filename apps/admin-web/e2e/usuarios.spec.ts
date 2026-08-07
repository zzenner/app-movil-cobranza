/**
 * Pruebas Playwright interceptadas (mock de red).
 * No requieren API real. Clasificación: INTERCEPTADO.
 */
import { test, expect, Page } from '@playwright/test';

const API_REFRESH = '**/api/v1/auth/web/refresh';
const API_ME = '**/api/v1/auth/me';
const API_USUARIOS = '**/api/v1/admin/usuarios**';

const perfilConPermiso = {
  usuarioId: '11111111-0000-0000-0000-000000000001',
  sesionId: '22222222-0000-0000-0000-000000000001',
  dispositivoId: null,
  tipoCliente: 'WEB',
  nombreUsuario: 'admin.test',
  roles: ['JEFE_SUPERVISORES'],
  permisos: ['USUARIOS_VER', 'USUARIOS_ADMINISTRAR'],
};

const perfilSinPermiso = {
  usuarioId: '11111111-0000-0000-0000-000000000002',
  sesionId: '22222222-0000-0000-0000-000000000002',
  dispositivoId: null,
  tipoCliente: 'WEB',
  nombreUsuario: 'sup.test',
  roles: ['SUPERVISOR'],
  permisos: [],
};

const loginResp = { accessToken: 'at-fake', expiresInSeconds: 900, sessionExpiresAt: '2026-11-01T00:00:00Z' };

const listadoMock = {
  contenido: [
    {
      id: '33333333-0000-0000-0000-000000000001',
      nombreUsuario: 'admin.test',
      nombres: 'Admin',
      apellidoPaterno: 'Test',
      apellidoMaterno: null,
      correo: null,
      estadoCalculado: 'ACTIVO',
      bloqueadoHasta: null,
      roles: ['JEFE_SUPERVISORES'],
      supervisorId: null,
      supervisorNombreUsuario: null,
      fechaCreacion: '2026-08-01T00:00:00Z',
    },
  ],
  pagina: 0,
  tamanio: 20,
  totalElementos: 1,
  totalPaginas: 1,
};

const detalleMock = {
  id: '33333333-0000-0000-0000-000000000001',
  nombreUsuario: 'admin.test',
  nombres: 'Admin',
  apellidoPaterno: 'Test',
  apellidoMaterno: null,
  correo: null,
  estadoCalculado: 'ACTIVO',
  activo: true,
  bloqueado: false,
  bloqueadoHasta: null,
  intentosFallidos: 0,
  fechaUltimoAcceso: null,
  roles: [{ codigo: 'JEFE_SUPERVISORES', fechaAsignacion: '2026-08-01T00:00:00Z' }],
  permisosEfectivos: ['USUARIOS_VER'],
  supervisorId: null,
  supervisorNombreUsuario: null,
  fechaCreacion: '2026-08-01T00:00:00Z',
  fechaActualizacion: '2026-08-01T00:00:00Z',
};

async function mockSesionConPermiso(page: Page) {
  await page.route(API_REFRESH, (r) => r.fulfill({ status: 200, json: loginResp }));
  await page.route(API_ME, (r) => r.fulfill({ status: 200, json: perfilConPermiso }));
}

async function mockSesionSinPermiso(page: Page) {
  await page.route(API_REFRESH, (r) => r.fulfill({ status: 200, json: loginResp }));
  await page.route(API_ME, (r) => r.fulfill({ status: 200, json: perfilSinPermiso }));
}

// ─── 1. Menú con permiso USUARIOS_VER ─────────────────────────────────────────

test('[INTERCEPTADO] menú Usuarios visible con permiso USUARIOS_VER', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(API_USUARIOS, (r) => r.fulfill({ status: 200, json: listadoMock }));
  await page.goto('/home');
  await expect(page.getByTestId('menu-usuarios')).toBeVisible();
});

// ─── 2. Menú oculto sin permiso ────────────────────────────────────────────────

test('[INTERCEPTADO] menú Usuarios oculto sin permiso USUARIOS_VER', async ({ page }) => {
  await mockSesionSinPermiso(page);
  await page.goto('/home');
  await expect(page.getByTestId('menu-usuarios')).not.toBeVisible();
});

// ─── 3. Acceso directo sin permiso lleva a /forbidden ─────────────────────────

test('[INTERCEPTADO] acceso directo a /usuarios sin permiso redirige a /forbidden', async ({ page }) => {
  await mockSesionSinPermiso(page);
  await page.goto('/usuarios');
  await expect(page).toHaveURL(/\/forbidden/);
});

// ─── 4. Listado carga usuarios ─────────────────────────────────────────────────

test('[INTERCEPTADO] listado de usuarios carga y muestra contenido', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(API_USUARIOS, (r) => r.fulfill({ status: 200, json: listadoMock }));
  await page.goto('/usuarios');
  await expect(page.getByRole('cell', { name: 'admin.test' })).toBeVisible();
});

// ─── 5. Filtros en query params persisten y carga datos ───────────────────────

test('[INTERCEPTADO] filtro estado en query params persiste y carga datos', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(API_USUARIOS, (r) => r.fulfill({ status: 200, json: listadoMock }));
  await page.goto('/usuarios?estado=ACTIVO');
  await expect(page).toHaveURL(/estado=ACTIVO/);
  await expect(page.getByText('admin.test').first()).toBeVisible();
});

// ─── 6. Detalle de usuario ─────────────────────────────────────────────────────

test('[INTERCEPTADO] detalle de usuario muestra datos del usuario', async ({ page }) => {
  const id = '33333333-0000-0000-0000-000000000001';
  await mockSesionConPermiso(page);
  await page.route(API_USUARIOS, (r) => r.fulfill({ status: 200, json: listadoMock }));
  await page.route(`**/api/v1/admin/usuarios/${id}`, (r) =>
    r.fulfill({ status: 200, json: detalleMock }),
  );
  await page.goto(`/usuarios/${id}`);
  await expect(page.getByRole('heading', { name: 'admin.test' })).toBeVisible();
  await expect(page.getByText('JEFE_SUPERVISORES').first()).toBeVisible();
});

// ─── 7. Volver conserva filtros ────────────────────────────────────────────────

test('[INTERCEPTADO] botón volver desde detalle conserva filtros del listado', async ({ page }) => {
  const id = '33333333-0000-0000-0000-000000000001';
  await mockSesionConPermiso(page);
  await page.route(API_USUARIOS, (r) => r.fulfill({ status: 200, json: listadoMock }));
  await page.route(`**/api/v1/admin/usuarios/${id}`, (r) =>
    r.fulfill({ status: 200, json: detalleMock }),
  );
  await page.goto(`/usuarios/${id}?pagina=1&estado=ACTIVO`);
  await page.waitForLoadState('networkidle');
  const botonVolver = page.getByRole('link', { name: /Volver/i }).first();
  const href = await botonVolver.getAttribute('href');
  expect(href).toContain('pagina=1');
  expect(href).toContain('estado=ACTIVO');
});

// ─── 8. Paginación ─────────────────────────────────────────────────────────────

test('[INTERCEPTADO] paginación muestra total de elementos', async ({ page }) => {
  await mockSesionConPermiso(page);
  const listadoConTotal = { ...listadoMock, totalElementos: 50, totalPaginas: 3 };
  await page.route(API_USUARIOS, (r) => r.fulfill({ status: 200, json: listadoConTotal }));
  await page.goto('/usuarios');
  await expect(page.locator('mat-paginator')).toBeVisible({ timeout: 10000 });
});

// ─── Fase 5B-2: Escritura de usuarios ──────────────────────────────────────────

const API_ROLES = '**/api/v1/admin/roles';
const ID_OTRO = '33333333-0000-0000-0000-000000000001';
const ID_PROPIO = perfilConPermiso.usuarioId; // '11111111-0000-0000-0000-000000000001'

const rolesLista = [
  { id: 'rol-001', codigo: 'JEFE_SUPERVISORES', nombre: 'Jefe de Supervisores' },
  { id: 'rol-002', codigo: 'SUPERVISOR', nombre: 'Supervisor' },
];

const detalleMockConAdmin = {
  ...detalleMock,
  permisosEfectivos: ['USUARIOS_VER', 'USUARIOS_ADMINISTRAR'],
  version: 0,
};

// ID diferente al de perfilConPermiso.usuarioId → esPropiasCuenta = false
const detalleMockOtroUsuario = { ...detalleMockConAdmin };

// ID igual al de perfilConPermiso.usuarioId → esPropiasCuenta = true
const detalleMockPropiasCuenta = {
  ...detalleMockConAdmin,
  id: perfilConPermiso.usuarioId,
};

const detalleMockInactivo = {
  ...detalleMockOtroUsuario,
  activo: false,
  estadoCalculado: 'INACTIVO',
};

// ─── 9. Botón Nuevo usuario visible con permiso USUARIOS_ADMINISTRAR ──────────

test('[INTERCEPTADO] crear usuario: botón Nuevo usuario visible con permiso USUARIOS_ADMINISTRAR', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(API_USUARIOS, (r) => r.fulfill({ status: 200, json: listadoMock }));
  await page.goto('/usuarios');
  await expect(page.getByTestId('btn-nuevo-usuario')).toBeVisible();
});

// ─── 10. Acceso sin permiso USUARIOS_ADMINISTRAR redirige a /forbidden ─────────

test('[INTERCEPTADO] crear usuario: acceso a /usuarios/nuevo sin permiso USUARIOS_ADMINISTRAR redirige a /forbidden', async ({ page }) => {
  await mockSesionSinPermiso(page);
  await page.goto('/usuarios/nuevo');
  await expect(page).toHaveURL(/\/forbidden/);
});

// ─── 11. Formulario nuevo usuario carga roles del catálogo ────────────────────

test('[INTERCEPTADO] crear usuario: formulario se carga con roles del catálogo', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(API_ROLES, (r) => r.fulfill({ status: 200, json: rolesLista }));
  await page.goto('/usuarios/nuevo');
  await expect(page.locator('mat-checkbox').first()).toBeVisible();
});

// ─── 12. Nombre duplicado muestra error ───────────────────────────────────────

test('[INTERCEPTADO] crear usuario: nombre duplicado muestra error', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(API_ROLES, (r) => r.fulfill({ status: 200, json: rolesLista }));
  await page.route('**/api/v1/admin/usuarios', (r) => {
    if (r.request().method() === 'POST') {
      r.fulfill({ status: 409, json: { code: 'NOMBRE_USUARIO_DUPLICADO' } });
    } else {
      r.continue();
    }
  });
  await page.goto('/usuarios/nuevo');
  await page.getByLabel('Nombre de usuario').fill('usuario.existente');
  await page.getByLabel('Contraseña inicial').fill('Contra123!');
  await page.getByLabel('Nombres').fill('Test');
  await page.getByLabel('Apellido paterno').fill('Usuario');
  await page.getByRole('checkbox').first().check();
  await page.getByRole('button', { name: 'Crear usuario' }).click();
  await expect(page.getByTestId('error-general')).toBeVisible();
});

// ─── 13. Editar: formulario carga datos del usuario ───────────────────────────

test('[INTERCEPTADO] editar usuario: formulario carga datos del usuario', async ({ page }) => {
  const id = ID_OTRO;
  await mockSesionConPermiso(page);
  await page.route(`**/api/v1/admin/usuarios/${id}`, (r) =>
    r.fulfill({ status: 200, json: detalleMockConAdmin }),
  );
  await page.goto(`/usuarios/${id}/editar`);
  await expect(page.locator('input[readonly]')).toHaveValue('admin.test');
  await expect(page.getByLabel('Nombres')).toHaveValue('Admin');
});

// ─── 14. Editar: conflicto de versión muestra mensaje ─────────────────────────

test('[INTERCEPTADO] editar usuario: conflicto de versión muestra mensaje', async ({ page }) => {
  const id = ID_OTRO;
  await mockSesionConPermiso(page);
  await page.route(`**/api/v1/admin/usuarios/${id}/datos-basicos`, (r) =>
    r.fulfill({ status: 409, json: { code: 'CONFLICTO_VERSION' } }),
  );
  await page.route(`**/api/v1/admin/usuarios/${id}`, (r) =>
    r.fulfill({ status: 200, json: detalleMockConAdmin }),
  );
  await page.goto(`/usuarios/${id}/editar`);
  await page.getByLabel('Nombres').fill('Nombre Actualizado');
  await page.getByRole('button', { name: 'Guardar cambios' }).click();
  await expect(page.getByTestId('error-general')).toContainText('modificados por otro');
});

// ─── 15. Detalle: botones de acciones administrativas visibles ────────────────

test('[INTERCEPTADO] detalle con permiso: botones de acciones administrativas visibles', async ({ page }) => {
  const id = ID_OTRO;
  await mockSesionConPermiso(page);
  await page.route(`**/api/v1/admin/usuarios/${id}`, (r) =>
    r.fulfill({ status: 200, json: detalleMockOtroUsuario }),
  );
  await page.goto(`/usuarios/${id}`);
  await expect(page.getByTestId('btn-editar')).toBeVisible();
  await expect(page.getByTestId('btn-desactivar')).toBeVisible();
  await expect(page.getByTestId('btn-bloquear')).toBeVisible();
  await expect(page.getByTestId('btn-reset-password')).toBeVisible();
});

// ─── 16. Detalle: botón desactivar oculto para propia cuenta ─────────────────

test('[INTERCEPTADO] detalle: desactivar botón oculto para propia cuenta', async ({ page }) => {
  const id = ID_PROPIO;
  await mockSesionConPermiso(page);
  await page.route(`**/api/v1/admin/usuarios/${id}`, (r) =>
    r.fulfill({ status: 200, json: detalleMockPropiasCuenta }),
  );
  await page.goto(`/usuarios/${id}`);
  await page.waitForLoadState('networkidle');
  await expect(page.getByTestId('btn-desactivar')).not.toBeVisible();
});

// ─── 17. Detalle: botón bloquear oculto para propia cuenta ───────────────────

test('[INTERCEPTADO] detalle: bloquear botón oculto para propia cuenta', async ({ page }) => {
  const id = ID_PROPIO;
  await mockSesionConPermiso(page);
  await page.route(`**/api/v1/admin/usuarios/${id}`, (r) =>
    r.fulfill({ status: 200, json: detalleMockPropiasCuenta }),
  );
  await page.goto(`/usuarios/${id}`);
  await page.waitForLoadState('networkidle');
  await expect(page.getByTestId('btn-bloquear')).not.toBeVisible();
});

// ─── 18. Detalle: activar usuario llama API y recarga ─────────────────────────

test('[INTERCEPTADO] detalle: activar usuario llama API y recarga', async ({ page }) => {
  const id = ID_OTRO;
  await mockSesionConPermiso(page);
  await page.route(`**/api/v1/admin/usuarios/${id}/activar`, (r) =>
    r.fulfill({ status: 204, body: '' }),
  );
  await page.route(`**/api/v1/admin/usuarios/${id}`, (r) =>
    r.fulfill({ status: 200, json: detalleMockInactivo }),
  );
  await page.goto(`/usuarios/${id}`);
  const activarPromise = page.waitForRequest(
    (req) => req.url().includes('/activar') && req.method() === 'POST',
  );
  await page.getByTestId('btn-activar').click();
  await page.locator('mat-dialog-container').getByRole('button', { name: 'Activar' }).click();
  const req = await activarPromise;
  expect(req.url()).toContain('/activar');
});

// ─── 19. Detalle: desactivar usuario muestra confirmación ─────────────────────

test('[INTERCEPTADO] detalle: desactivar usuario muestra confirmación', async ({ page }) => {
  const id = ID_OTRO;
  await mockSesionConPermiso(page);
  await page.route(`**/api/v1/admin/usuarios/${id}`, (r) =>
    r.fulfill({ status: 200, json: detalleMockOtroUsuario }),
  );
  await page.goto(`/usuarios/${id}`);
  await page.getByTestId('btn-desactivar').click();
  await expect(page.locator('mat-dialog-container')).toContainText('Desactivar');
});

// ─── 20. Detalle: restablecer contraseña muestra diálogo ─────────────────────

test('[INTERCEPTADO] detalle: restablecer contraseña muestra diálogo de contraseña', async ({ page }) => {
  const id = ID_OTRO;
  await mockSesionConPermiso(page);
  await page.route(`**/api/v1/admin/usuarios/${id}`, (r) =>
    r.fulfill({ status: 200, json: detalleMockOtroUsuario }),
  );
  await page.goto(`/usuarios/${id}`);
  await page.getByTestId('btn-reset-password').click();
  await expect(page.locator('mat-dialog-container')).toContainText('contraseña');
});
