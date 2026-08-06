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
