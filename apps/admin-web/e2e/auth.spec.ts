import { test, expect, Page } from '@playwright/test';

const API_LOGIN = '**/api/v1/auth/web/login';
const API_REFRESH = '**/api/v1/auth/web/refresh';
const API_ME = '**/api/v1/auth/me';
const API_LOGOUT = '**/api/v1/auth/web/logout';

const mockProfile = {
  usuarioId: '11111111-0000-0000-0000-000000000001',
  sesionId: '22222222-0000-0000-0000-000000000001',
  dispositivoId: null,
  tipoCliente: 'WEB',
  nombreUsuario: 'admin.test',
  roles: ['ADMINISTRADOR'],
  permisos: ['USUARIOS_VER'],
};

const mockLoginResp = {
  accessToken: 'at-fake-token',
  expiresInSeconds: 900,
  sessionExpiresAt: '2026-11-01T00:00:00Z',
};

/** Intercepta refresh como fallido (primera carga, sin sesión previa) */
async function mockRefreshFail(page: Page) {
  await page.route(API_REFRESH, (route) =>
    route.fulfill({ status: 401, json: { error: 'Unauthorized' } }),
  );
}

/** Intercepta refresh como exitoso */
async function mockRefreshOk(page: Page) {
  await page.route(API_REFRESH, (route) => route.fulfill({ status: 200, json: mockLoginResp }));
  await page.route(API_ME, (route) => route.fulfill({ status: 200, json: mockProfile }));
}

/** Intercepta login como exitoso */
async function mockLoginOk(page: Page) {
  await page.route(API_LOGIN, (route) => route.fulfill({ status: 200, json: mockLoginResp }));
  await page.route(API_ME, (route) => route.fulfill({ status: 200, json: mockProfile }));
}

/** Intercepta login como fallido */
async function mockLoginFail(page: Page) {
  await page.route(API_LOGIN, (route) =>
    route.fulfill({ status: 401, json: { error: 'Unauthorized' } }),
  );
}

// ─── 1. Redirige a /login sin sesión ─────────────────────────────────────────

test('redirige a /login si no hay sesión activa', async ({ page }) => {
  await mockRefreshFail(page);
  await page.goto('/');
  await expect(page).toHaveURL(/\/login/);
});

// ─── 2. Login exitoso navega a /home ─────────────────────────────────────────

test('login exitoso navega a /home y muestra usuario', async ({ page }) => {
  await mockRefreshFail(page);
  await mockLoginOk(page);

  await page.goto('/login');
  await page.getByTestId('username-input').fill('admin.test');
  await page.getByTestId('password-input').fill('ClaveTest.123!');
  await page.getByTestId('submit-button').click();

  await expect(page).toHaveURL(/\/home/);
  await expect(page.locator('mat-card-title').filter({ hasText: 'admin.test' })).toBeVisible();
});

// ─── 3. Login con credenciales incorrectas muestra error ──────────────────────

test('credenciales incorrectas muestran mensaje de error', async ({ page }) => {
  await mockRefreshFail(page);
  await mockLoginFail(page);

  await page.goto('/login');
  await page.getByTestId('username-input').fill('usuario');
  await page.getByTestId('password-input').fill('clave-mala');
  await page.getByTestId('submit-button').click();

  await expect(page.getByTestId('error-message')).toBeVisible();
  await expect(page.getByTestId('error-message')).toContainText('incorrectos');
});

// ─── 4. Doble submit no dispara dos peticiones ────────────────────────────────

test('botón deshabilitado durante login evita doble submit', async ({ page }) => {
  await mockRefreshFail(page);

  let requestCount = 0;
  await page.route(API_LOGIN, async (route) => {
    requestCount++;
    await new Promise((r) => setTimeout(r, 200));
    await route.fulfill({ status: 200, json: mockLoginResp });
  });
  await page.route(API_ME, (route) => route.fulfill({ status: 200, json: mockProfile }));

  await page.goto('/login');
  await page.getByTestId('username-input').fill('admin');
  await page.getByTestId('password-input').fill('clave');

  await page.getByTestId('submit-button').click();

  // El botón queda deshabilitado (loading=true) mientras la petición está en vuelo
  await expect(page.getByTestId('submit-button')).toBeDisabled();

  await expect(page).toHaveURL(/\/home/);
  expect(requestCount).toBe(1);
});

// ─── 5. Sesión existente redirige de /login a /home ──────────────────────────

test('usuario con sesión activa redirigido de /login a /home', async ({ page }) => {
  await mockRefreshOk(page);
  await page.goto('/login');
  await expect(page).toHaveURL(/\/home/);
});

// ─── 6. Logout redirige a /login ─────────────────────────────────────────────

test('logout redirige a /login', async ({ page }) => {
  await mockRefreshOk(page);
  await page.route(API_LOGOUT, (route) => route.fulfill({ status: 204 }));

  await page.goto('/home');
  await expect(page).toHaveURL(/\/home/);

  await page.getByTitle('Cerrar sesión').click();
  await expect(page).toHaveURL(/\/login/);
});
