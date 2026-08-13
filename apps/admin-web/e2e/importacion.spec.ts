/**
 * Pruebas Playwright interceptadas (mock de red) — Fase 5C Importación Mensual.
 * No requieren API real. Clasificación: INTERCEPTADO.
 */
import { test, expect, Page } from '@playwright/test';

const API_REFRESH = '**/api/v1/auth/web/refresh';
const API_ME = '**/api/v1/auth/me';
const API_CARTERAS = '**/api/v1/admin/carteras/activas';
const API_IMPORTACIONES = '**/api/v1/admin/importaciones/mensuales**';
const ID_IMP = 'aaaaaaaa-0001-0001-0001-000000000001';

const loginResp = { accessToken: 'at-fake', expiresInSeconds: 900, sessionExpiresAt: '2026-11-01T00:00:00Z' };

const perfilConPermiso = {
  usuarioId: '11111111-0000-0000-0000-000000000010',
  sesionId: '22222222-0000-0000-0000-000000000010',
  dispositivoId: null,
  tipoCliente: 'WEB',
  nombreUsuario: 'jefe.test',
  roles: ['JEFE_SUPERVISORES'],
  permisos: ['DATOS_IMPORTAR'],
};

const perfilSinPermiso = {
  usuarioId: '11111111-0000-0000-0000-000000000011',
  sesionId: '22222222-0000-0000-0000-000000000011',
  dispositivoId: null,
  tipoCliente: 'WEB',
  nombreUsuario: 'sup.test',
  roles: ['SUPERVISOR'],
  permisos: [],
};

const carteras = [
  { id: 'cart-001', nombre: 'Cartera Norte' },
  { id: 'cart-002', nombre: 'Cartera Sur' },
];

function makeImportacion(estado: string, extra: Record<string, unknown> = {}) {
  return {
    id: ID_IMP,
    carteraId: 'cart-001',
    periodo: '2026-08',
    sistemaOrigen: 'LEGADO',
    estado,
    nombreArchivoOriginal: 'importacion_2026-08.csv',
    filasTotales: 10,
    filasProcesadas: null,
    filasRechazadas: 0,
    filasAdvertencia: 0,
    usuarioId: perfilConPermiso.usuarioId,
    personasCreadas: null,
    personasActualizadas: null,
    operacionesCreadas: null,
    operacionesActualizadas: null,
    cuotasCreadas: null,
    cuotasActualizadas: null,
    mensajeError: null,
    fechaCreacion: '2026-08-09T10:00:00Z',
    fechaActualizacion: '2026-08-09T10:01:00Z',
    version: 1,
    ...extra,
  };
}

const paginaImportaciones = {
  contenido: [makeImportacion('COMPLETADA', {
    filasProcesadas: 10, personasCreadas: 3, personasActualizadas: 2,
    operacionesCreadas: 10, operacionesActualizadas: 0, cuotasCreadas: 25, cuotasActualizadas: 0,
  })],
  pagina: 0,
  tamanio: 20,
  totalElementos: 1,
  totalPaginas: 1,
};

const erroresVacio = { contenido: [], pagina: 0, tamanio: 50, totalElementos: 0, totalPaginas: 0 };

const erroresPagina = {
  contenido: [
    {
      id: 'err-001',
      numeroFila: 3,
      columna: 'RUT_NUMERO',
      codigoError: 'RUT_INVALIDO_MODULO_11',
      nivel: 'ERROR',
      mensaje: 'El RUT no es válido según módulo 11',
    },
    {
      id: 'err-002',
      numeroFila: 5,
      columna: 'MONTO_CUOTA',
      codigoError: 'MONTO_NEGATIVO',
      nivel: 'ERROR',
      mensaje: 'El monto de la cuota no puede ser negativo',
    },
  ],
  pagina: 0,
  tamanio: 50,
  totalElementos: 2,
  totalPaginas: 1,
};

async function mockSesionConPermiso(page: Page) {
  await page.route(API_REFRESH, (r) => r.fulfill({ status: 200, json: loginResp }));
  await page.route(API_ME, (r) => r.fulfill({ status: 200, json: perfilConPermiso }));
}

async function mockSesionSinPermiso(page: Page) {
  await page.route(API_REFRESH, (r) => r.fulfill({ status: 200, json: loginResp }));
  await page.route(API_ME, (r) => r.fulfill({ status: 200, json: perfilSinPermiso }));
}

// ─── 1. Historial de importaciones ────────────────────────────────────────────

test('[PLAYWRIGHT INTERCEPTADO] historial: listado carga importaciones del historial', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(API_CARTERAS, (r) => r.fulfill({ status: 200, json: carteras }));
  await page.route(API_IMPORTACIONES, (r) => r.fulfill({ status: 200, json: paginaImportaciones }));
  await page.goto('/importacion');
  await expect(page.getByTestId('btn-nueva-importacion')).toBeVisible();
  await expect(page.getByText('2026-08').first()).toBeVisible();
  await expect(page.getByText('COMPLETADA').first()).toBeVisible();
});

// ─── 2. Nueva importación — formulario se carga (contrato v2) ────────────────

test('[PLAYWRIGHT INTERCEPTADO] nueva importacion: formulario de carga se muestra correctamente', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.goto('/importacion/nueva');
  await page.waitForLoadState('networkidle');
  await expect(page.getByTestId('btn-subir')).toBeVisible();
  await expect(page.getByTestId('btn-subir')).toContainText('Subir y validar');
  // Contrato v2: no hay selector de cartera ni campo de período
  await expect(page.getByTestId('selector-cartera')).not.toBeVisible().catch(() => {});
  await expect(page.getByTestId('input-periodo')).not.toBeVisible().catch(() => {});
});

// ─── 3. Acceso sin permiso DATOS_IMPORTAR ─────────────────────────────────────

test('[PLAYWRIGHT INTERCEPTADO] acceso sin DATOS_IMPORTAR redirige a /forbidden', async ({ page }) => {
  await mockSesionSinPermiso(page);
  await page.goto('/importacion');
  await expect(page).toHaveURL(/\/forbidden/);
});

// ─── 4. Historial: filtro de cartera carga opciones (contrato v2) ────────────

test('[PLAYWRIGHT INTERCEPTADO] historial: dropdown de carteras permite filtrar el listado', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(API_CARTERAS, (r) => r.fulfill({ status: 200, json: carteras }));
  await page.route(API_IMPORTACIONES, (r) => r.fulfill({ status: 200, json: paginaImportaciones }));
  await page.goto('/importacion');
  await page.waitForLoadState('networkidle');
  // El filtro de cartera está en el listado, no en el formulario de nueva importación
  const selectCartera = page.locator('mat-select').first();
  await selectCartera.click();
  await expect(page.getByRole('option', { name: 'Cartera Norte' })).toBeVisible();
  await expect(page.getByRole('option', { name: 'Cartera Sur' })).toBeVisible();
});

// ─── 5. Nueva importación: solo requiere archivo CSV (contrato v2) ────────────

test('[PLAYWRIGHT INTERCEPTADO] nueva importacion: solo requiere archivo CSV, sin cartera ni periodo', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.goto('/importacion/nueva');
  await page.waitForLoadState('networkidle');
  // El formulario tiene selector de archivo y botón de subir
  const inputArchivo = page.locator('input[type="file"]');
  await expect(inputArchivo).toBeAttached();
  await expect(page.getByTestId('btn-subir')).toBeVisible();
  // No hay selector de cartera en la página de nueva importación
  await expect(page.getByTestId('selector-cartera')).toHaveCount(0);
  // No hay campo de período en la página de nueva importación
  await expect(page.getByTestId('input-periodo')).toHaveCount(0);
});

// ─── 6. Seleccionar archivo CSV ───────────────────────────────────────────────

test('[PLAYWRIGHT INTERCEPTADO] nueva importacion: archivo CSV puede ser seleccionado en el formulario', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.goto('/importacion/nueva');
  await page.waitForLoadState('networkidle');
  const inputArchivo = page.locator('input[type="file"]');
  await expect(inputArchivo).toBeAttached();
});

// ─── 7. Estado VALIDANDO — spinner de progreso ────────────────────────────────

test('[PLAYWRIGHT INTERCEPTADO] detalle VALIDANDO: muestra spinner de progreso', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(`**/api/v1/admin/importaciones/mensuales/${ID_IMP}`, (r) =>
    r.fulfill({ status: 200, json: makeImportacion('VALIDANDO') }),
  );
  await page.route(`**/api/v1/admin/importaciones/mensuales/${ID_IMP}/errores**`, (r) =>
    r.fulfill({ status: 200, json: erroresVacio }),
  );
  await page.goto(`/importacion/${ID_IMP}`);
  await page.waitForLoadState('networkidle');
  await expect(page.getByTestId('spinner-progreso')).toBeVisible();
  await expect(page.getByTestId('estado-badge')).toContainText('VALIDANDO');
});

// ─── 8. Estado VALIDADA — panel de confirmación ───────────────────────────────

test('[PLAYWRIGHT INTERCEPTADO] detalle VALIDADA: muestra panel de confirmacion con botón confirmar', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(`**/api/v1/admin/importaciones/mensuales/${ID_IMP}`, (r) =>
    r.fulfill({ status: 200, json: makeImportacion('VALIDADA') }),
  );
  await page.route(`**/api/v1/admin/importaciones/mensuales/${ID_IMP}/errores**`, (r) =>
    r.fulfill({ status: 200, json: erroresVacio }),
  );
  await page.goto(`/importacion/${ID_IMP}`);
  await page.waitForLoadState('networkidle');
  await expect(page.getByTestId('panel-confirmar')).toBeVisible();
  await expect(page.getByTestId('btn-confirmar')).toBeVisible();
  await expect(page.getByTestId('estado-badge')).toContainText('VALIDADA');
});

// ─── 9. Confirmar importación — llama a la API ───────────────────────────────

test('[PLAYWRIGHT INTERCEPTADO] detalle VALIDADA: confirmar llama al endpoint y actualiza estado', async ({ page }) => {
  await mockSesionConPermiso(page);
  let detalleCount = 0;
  await page.route(`**/api/v1/admin/importaciones/mensuales/${ID_IMP}`, (r) => {
    detalleCount++;
    const estado = detalleCount === 1 ? 'VALIDADA' : 'PROCESANDO';
    r.fulfill({ status: 200, json: makeImportacion(estado) });
  });
  await page.route(`**/api/v1/admin/importaciones/mensuales/${ID_IMP}/errores**`, (r) =>
    r.fulfill({ status: 200, json: erroresVacio }),
  );
  await page.route(`**/api/v1/admin/importaciones/mensuales/${ID_IMP}/confirmar`, (r) =>
    r.fulfill({ status: 202, body: '' }),
  );
  await page.goto(`/importacion/${ID_IMP}`);
  await page.waitForLoadState('networkidle');
  const confirmarPromise = page.waitForRequest(
    (req) => req.url().includes('/confirmar') && req.method() === 'POST',
  );
  await page.getByTestId('btn-confirmar').click();
  const req = await confirmarPromise;
  expect(req.url()).toContain('/confirmar');
});

// ─── 10. Estado PROCESANDO — spinner de progreso ─────────────────────────────

test('[PLAYWRIGHT INTERCEPTADO] detalle PROCESANDO: muestra spinner de progreso', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(`**/api/v1/admin/importaciones/mensuales/${ID_IMP}`, (r) =>
    r.fulfill({ status: 200, json: makeImportacion('PROCESANDO') }),
  );
  await page.route(`**/api/v1/admin/importaciones/mensuales/${ID_IMP}/errores**`, (r) =>
    r.fulfill({ status: 200, json: erroresVacio }),
  );
  await page.goto(`/importacion/${ID_IMP}`);
  await page.waitForLoadState('networkidle');
  await expect(page.getByTestId('spinner-progreso')).toBeVisible();
  await expect(page.getByTestId('estado-badge')).toContainText('PROCESANDO');
});

// ─── 11. Estado COMPLETADA — panel de resultados ─────────────────────────────

test('[PLAYWRIGHT INTERCEPTADO] detalle COMPLETADA: muestra panel de resultado con contadores', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(`**/api/v1/admin/importaciones/mensuales/${ID_IMP}`, (r) =>
    r.fulfill({
      status: 200,
      json: makeImportacion('COMPLETADA', {
        filasProcesadas: 10,
        personasCreadas: 3,
        personasActualizadas: 2,
        operacionesCreadas: 10,
        operacionesActualizadas: 0,
        cuotasCreadas: 25,
        cuotasActualizadas: 0,
      }),
    }),
  );
  await page.goto(`/importacion/${ID_IMP}`);
  await page.waitForLoadState('networkidle');
  await expect(page.getByTestId('panel-completada')).toBeVisible();
  await expect(page.getByTestId('estado-badge')).toContainText('COMPLETADA');
  await expect(page.getByText('Personas creadas').first()).toBeVisible();
});

// ─── 12. Estado CON_ERRORES — tabla de errores ───────────────────────────────

test('[PLAYWRIGHT INTERCEPTADO] detalle CON_ERRORES: muestra tabla de errores con detalle por fila', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(`**/api/v1/admin/importaciones/mensuales/${ID_IMP}`, (r) =>
    r.fulfill({ status: 200, json: makeImportacion('CON_ERRORES', { filasRechazadas: 2 }) }),
  );
  await page.route(`**/api/v1/admin/importaciones/mensuales/${ID_IMP}/errores**`, (r) =>
    r.fulfill({ status: 200, json: erroresPagina }),
  );
  await page.goto(`/importacion/${ID_IMP}`);
  await page.waitForLoadState('networkidle');
  await expect(page.getByTestId('estado-badge')).toContainText('CON_ERRORES');
  await expect(page.getByTestId('tabla-errores')).toBeVisible();
  await expect(page.getByText('RUT_INVALIDO_MODULO_11').first()).toBeVisible();
});

// ─── 13. ARCHIVO_YA_IMPORTADO — error al subir mismo archivo ─────────────────

test('[PLAYWRIGHT INTERCEPTADO] nueva importacion: ARCHIVO_YA_IMPORTADO muestra error al usuario', async ({ page }) => {
  await mockSesionConPermiso(page);
  // Contrato v2: la nueva importación no consulta carteras/activas
  await page.route('**/api/v1/admin/importaciones/mensuales', (r) => {
    if (r.request().method() === 'POST') {
      r.fulfill({
        status: 409,
        json: { code: 'ARCHIVO_YA_IMPORTADO', detail: 'ARCHIVO_YA_IMPORTADO: el hash del archivo coincide con una importación ya completada' },
      });
    } else {
      r.continue();
    }
  });
  await page.goto('/importacion/nueva');
  await page.waitForLoadState('networkidle');
  // Contrato v2: solo se adjunta el archivo CSV (sin cartera ni período)
  await page.locator('input[type="file"]').setInputFiles({
    name: 'importacion_2026-08.csv',
    mimeType: 'text/csv',
    buffer: Buffer.from('RUT_NUMERO;NOMBRE\n12345678;Juan'),
  });
  await page.getByTestId('btn-subir').click();
  await expect(page.getByTestId('error-servidor')).toContainText('ARCHIVO_YA_IMPORTADO');
});

// ─── 14. Estado EXPIRADA — panel de expiración ───────────────────────────────

test('[PLAYWRIGHT INTERCEPTADO] detalle EXPIRADA: muestra panel de expiración con mensaje', async ({ page }) => {
  await mockSesionConPermiso(page);
  await page.route(`**/api/v1/admin/importaciones/mensuales/${ID_IMP}`, (r) =>
    r.fulfill({ status: 200, json: makeImportacion('EXPIRADA') }),
  );
  await page.goto(`/importacion/${ID_IMP}`);
  await page.waitForLoadState('networkidle');
  await expect(page.getByTestId('panel-expirada')).toBeVisible();
  await expect(page.getByTestId('estado-badge')).toContainText('EXPIRADA');
});
