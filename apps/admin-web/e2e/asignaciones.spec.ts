/**
 * Pruebas Playwright interceptadas (mock de red) — Fase 6B Asignaciones Diarias.
 * No requieren API real. Clasificación: INTERCEPTADO.
 */
import { test, expect, Page } from '@playwright/test';

const API_REFRESH = '**/api/v1/auth/web/refresh';
const API_ME = '**/api/v1/auth/me';
const API_DIARIAS = '**/api/v1/admin/asignaciones/diarias**';
const API_DIARIA_ID = '**/api/v1/admin/asignaciones/diarias/aaaaaaaa-*';
const API_PERIODOS = '**/api/v1/admin/asignaciones/periodos**';
const API_MENSUALES = '**/api/v1/admin/asignaciones/mensuales**';
const API_PERSONAS_DISPONIBLES = '**/api/v1/admin/asignaciones/mensuales/**/personas-disponibles';
const ID_DIARIA = 'aaaaaaaa-6b00-6b00-6b00-000000000001';
const ID_MENSUAL = 'bbbbbbbb-6b00-6b00-6b00-000000000001';

const loginResp = { accessToken: 'at-fake', expiresInSeconds: 900, sessionExpiresAt: '2026-11-01T00:00:00Z' };

const perfilConPermiso = {
  usuarioId: '11111111-6b00-0000-0000-000000000010',
  sesionId: '22222222-6b00-0000-0000-000000000010',
  dispositivoId: null,
  tipoCliente: 'WEB',
  nombreUsuario: 'sup.test',
  roles: ['SUPERVISOR'],
  permisos: ['ASIGNACIONES_VER', 'ASIGNACIONES_ADMINISTRAR'],
};

const perfilSinPermiso = {
  usuarioId: '11111111-6b00-0000-0000-000000000011',
  sesionId: '22222222-6b00-0000-0000-000000000011',
  dispositivoId: null,
  tipoCliente: 'WEB',
  nombreUsuario: 'ejec.test',
  roles: ['EJECUTIVO_TERRENO'],
  permisos: [],
};

const periodos = [{ periodo: '2026-08' }, { periodo: '2026-07' }];

const mensuales = [
  {
    id: ID_MENSUAL,
    periodo: '2026-08',
    carteraId: 'cart-001',
    nombreCartera: 'Cartera Norte',
    ejecutivoId: 'ejec-001',
    nombreEjecutivo: 'Pedro Soto',
    codigoEjecutivo: 'EJ-001',
    supervisorId: perfilConPermiso.usuarioId,
    nombreSupervisor: 'Ana Torres',
    cantidadPersonas: 5,
  },
];

const personasDisponibles = [
  {
    personaId: 'persona-001',
    rutNumero: '15000001',
    rutDv: '7',
    nombre: 'Carlos Rojas',
    carteraId: 'cart-001',
    nombreCartera: 'Cartera Norte',
    cantidadOperaciones: 2,
    tieneAsignacionDiaria: false,
  },
];

const listaDiarias = [
  {
    id: ID_DIARIA,
    fecha: '2026-08-13',
    periodo: '2026-08',
    carteraId: 'cart-001',
    nombreCartera: 'Cartera Norte',
    ejecutivoId: 'ejec-001',
    nombreEjecutivo: 'Pedro Soto',
    supervisorId: perfilConPermiso.usuarioId,
    nombreSupervisor: 'Ana Torres',
    estado: 'BORRADOR',
    fechaPublicacion: null,
    cantidadPersonas: 1,
  },
];

function makeDetalle(estado: string, extra: Record<string, unknown> = {}) {
  return {
    id: ID_DIARIA,
    fecha: '2026-08-13',
    periodo: '2026-08',
    carteraId: 'cart-001',
    nombreCartera: 'Cartera Norte',
    ejecutivoId: 'ejec-001',
    nombreEjecutivo: 'Pedro Soto',
    supervisorId: perfilConPermiso.usuarioId,
    nombreSupervisor: 'Ana Torres',
    estado,
    fechaPublicacion: estado === 'PUBLICADA' ? '2026-08-13T10:00:00Z' : null,
    publicadoPorId: estado === 'PUBLICADA' ? perfilConPermiso.usuarioId : null,
    nombrePublicador: estado === 'PUBLICADA' ? 'Ana Torres' : null,
    motivoCancelacion: null,
    fechaCreacion: '2026-08-13T09:00:00Z',
    version: 1,
    cantidadPersonas: 1,
    personas: [{ personaId: 'persona-001', rutNumero: '15000001', rutDv: '7', nombre: 'Carlos Rojas' }],
    ...extra,
  };
}

async function mockSesionConPermiso(page: Page) {
  await page.route(API_REFRESH, (r) => r.fulfill({ status: 200, json: loginResp }));
  await page.route(API_ME, (r) => r.fulfill({ status: 200, json: perfilConPermiso }));
}

async function mockSesionSinPermiso(page: Page) {
  await page.route(API_REFRESH, (r) => r.fulfill({ status: 200, json: loginResp }));
  await page.route(API_ME, (r) => r.fulfill({ status: 200, json: perfilSinPermiso }));
}

// ─── 1. Menú visible con permiso ASIGNACIONES_VER ─────────────────────────────

test('[INTERCEPTADO] menú Asignaciones visible con permiso ASIGNACIONES_VER', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(API_DIARIAS, (r) => r.fulfill({ status: 200, json: listaDiarias }));
  await page.goto('/asignaciones');
  await expect(page.getByTestId('menu-asignaciones')).toBeVisible();
});

// ─── 2. Menú oculto sin permiso ───────────────────────────────────────────────

test('[INTERCEPTADO] menú Asignaciones oculto sin permiso ASIGNACIONES_VER', async ({ page }) => {
  await mockSesionSinPermiso(page);
  await page.goto('/forbidden');
  await expect(page.getByTestId('menu-asignaciones')).toHaveCount(0);
});

// ─── 3. Acceso directo sin permiso redirige a /forbidden ──────────────────────

test('[INTERCEPTADO] acceso a /asignaciones sin permiso redirige a /forbidden', async ({ page }) => {
  await mockSesionSinPermiso(page);
  await page.goto('/asignaciones');
  await expect(page).toHaveURL(/\/forbidden/);
});

// ─── 4. Listado de asignaciones carga y muestra datos ─────────────────────────

test('[INTERCEPTADO] listado de asignaciones diarias carga y muestra filas', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(API_DIARIAS, (r) => r.fulfill({ status: 200, json: listaDiarias }));
  await page.goto('/asignaciones');
  await page.waitForLoadState('networkidle');
  await expect(page.getByTestId('tabla-asignaciones')).toBeVisible();
  await expect(page.getByText('Pedro Soto').first()).toBeVisible();
  await expect(page.getByText('BORRADOR').first()).toBeVisible();
});

// ─── 5. Listado: botón Nueva asignación visible con ASIGNACIONES_ADMINISTRAR ──

test('[INTERCEPTADO] listado: botón nueva asignación visible con ASIGNACIONES_ADMINISTRAR', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(API_DIARIAS, (r) => r.fulfill({ status: 200, json: listaDiarias }));
  await page.goto('/asignaciones');
  await page.waitForLoadState('networkidle');
  await expect(page.getByTestId('btn-nueva-asignacion')).toBeVisible();
});

// ─── 6. Formulario crear: carga períodos disponibles ─────────────────────────

test('[INTERCEPTADO] crear: formulario carga períodos disponibles en el paso 1', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(API_PERIODOS, (r) => r.fulfill({ status: 200, json: periodos }));
  await page.route(API_MENSUALES, (r) => r.fulfill({ status: 200, json: mensuales }));
  await page.goto('/asignaciones/crear');
  await page.waitForLoadState('networkidle');
  const selPeriodo = page.getByTestId('sel-periodo');
  await selPeriodo.click();
  await expect(page.getByRole('option', { name: '2026-08' })).toBeVisible();
  await expect(page.getByRole('option', { name: '2026-07' })).toBeVisible();
});

// ─── 7. Formulario crear: seleccionar período carga ejecutivos de la mensual ──

test('[INTERCEPTADO] crear: seleccionar período muestra ejecutivos en el selector de mensual', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(API_PERIODOS, (r) => r.fulfill({ status: 200, json: periodos }));
  await page.route(API_MENSUALES, (r) => r.fulfill({ status: 200, json: mensuales }));
  await page.goto('/asignaciones/crear');
  await page.waitForLoadState('networkidle');
  const selPeriodo = page.getByTestId('sel-periodo');
  await selPeriodo.click();
  await page.getByRole('option', { name: '2026-08' }).click();
  const selMensual = page.getByTestId('sel-mensual');
  await selMensual.click();
  await expect(page.getByRole('option', { name: /Pedro Soto/ })).toBeVisible();
});

// ─── 8. Formulario crear: carga personas disponibles al avanzar ───────────────

test('[INTERCEPTADO] crear: paso 2 muestra personas disponibles de la mensual', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(API_PERIODOS, (r) => r.fulfill({ status: 200, json: periodos }));
  await page.route(API_MENSUALES, (r) => r.fulfill({ status: 200, json: mensuales }));
  await page.route(API_PERSONAS_DISPONIBLES, (r) =>
    r.fulfill({ status: 200, json: personasDisponibles }),
  );
  await page.goto('/asignaciones/crear');
  await page.waitForLoadState('networkidle');
  // Paso 1: seleccionar período
  await page.getByTestId('sel-periodo').click();
  await page.getByRole('option', { name: '2026-08' }).click();
  // Seleccionar mensual
  await page.getByTestId('sel-mensual').click();
  await page.getByRole('option', { name: /Pedro Soto/ }).click();
  // Seleccionar fecha
  await page.getByTestId('input-fecha').fill('2026-08-13');
  // Avanzar al paso 2
  await page.getByTestId('btn-siguiente-paso1').click();
  await page.waitForLoadState('networkidle');
  // Verificar que aparece el nombre de una persona
  await expect(page.getByText('Carlos Rojas')).toBeVisible();
});

// ─── 9. Detalle borrador: muestra estado BORRADOR y botones publicar/cancelar ─

test('[INTERCEPTADO] detalle BORRADOR: muestra badge y botones publicar y cancelar', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(
    (url) => url.href.includes(`/api/v1/admin/asignaciones/diarias/${ID_DIARIA}`),
    (r) => r.fulfill({ status: 200, json: makeDetalle('BORRADOR') }),
  );
  await page.goto(`/asignaciones/${ID_DIARIA}`);
  await page.waitForLoadState('networkidle');
  await expect(page.getByTestId('badge-estado')).toContainText('Borrador');
  await expect(page.getByTestId('btn-publicar')).toBeVisible();
  await expect(page.getByTestId('btn-cancelar')).toBeVisible();
});

// ─── 10. Detalle PUBLICADA: sin botón publicar, con botón cancelar publicada ──

test('[INTERCEPTADO] detalle PUBLICADA: sin botón publicar, con botón cancelar-publicada', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(
    (url) => url.href.includes(`/api/v1/admin/asignaciones/diarias/${ID_DIARIA}`),
    (r) => r.fulfill({ status: 200, json: makeDetalle('PUBLICADA') }),
  );
  await page.goto(`/asignaciones/${ID_DIARIA}`);
  await page.waitForLoadState('networkidle');
  await expect(page.getByTestId('badge-estado')).toContainText('Publicada');
  await expect(page.getByTestId('btn-publicar')).toHaveCount(0);
  await expect(page.getByTestId('btn-cancelar-publicada')).toBeVisible();
});

// ─── 11. Publicar asignación desde detalle llama API y muestra banner éxito ──

test('[INTERCEPTADO] publicar: clic en publicar llama a la API y muestra banner de éxito', async ({ page }) => {
  await mockSesionConPermiso(page);
  // Aceptar el confirm() nativo que dispara publicar()
  page.on('dialog', (dialog) => dialog.accept());

  let llamadas = 0;
  await page.route(
    (url) => url.href.includes(`/api/v1/admin/asignaciones/diarias/${ID_DIARIA}`),
    (r) => {
      if (r.request().url().endsWith('/publicar')) {
        r.fulfill({ status: 204 });
      } else {
        llamadas++;
        r.fulfill({ status: 200, json: makeDetalle(llamadas === 1 ? 'BORRADOR' : 'PUBLICADA') });
      }
    },
  );

  await page.goto(`/asignaciones/${ID_DIARIA}`);
  await page.waitForLoadState('networkidle');
  await expect(page.getByTestId('btn-publicar')).toBeVisible();
  await page.getByTestId('btn-publicar').click();
  await expect(page.getByTestId('banner-exito')).toBeVisible({ timeout: 5000 });
});

// ─── 12. Listado: error de API muestra mensaje de error ───────────────────────

test('[INTERCEPTADO] listado: error de API muestra mensaje al usuario', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(API_DIARIAS, (r) =>
    r.fulfill({
      status: 500,
      json: { status: 500, title: 'Error interno', code: 'ERROR_INTERNO' },
      contentType: 'application/problem+json',
    }),
  );
  await page.goto('/asignaciones');
  await page.waitForLoadState('networkidle');
  await expect(page.locator('.error-msg')).toBeVisible();
});
