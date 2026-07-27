# Alcance del proyecto

## Incluido en la Fase 1 (MVP)

### Estructura de datos central
- Unidad principal: persona (`cartera → persona → operaciones → cuotas`).
- RUT almacenado como `rut_numero` + `rut_dv`.
- Personas con operaciones, cuotas, avales y direcciones.
- Importación de asignaciones mensuales por CSV.
- Asignación diaria (~50 personas) como unidad de trabajo y descarga al móvil.
- Historial completo de asignaciones (mensuales y diarias).

### Roles y usuarios
- Roles: `JEFE_SUPERVISORES`, `TECNOLOGIA`, `SUPERVISOR`, `EJECUTIVO_TERRENO`.
- Relación de supervisión con historial.

### Aplicación Android
- Login online con credenciales institucionales.
- Persistencia de sesión offline (mecanismo exacto: **PENDIENTE**).
- Descarga de la asignación diaria vigente y sus datos relacionados.
- Consulta local de personas por RUT (en asignación diaria).
- Consulta global por RUT mediante API cuando haya conexión.
- Visualización de personas, operaciones y cuotas.
- Registro de gestiones en terreno:
  - Tipos: `CONTACTO_FAMILIAR`, `COMPROMISO_PAGO`, `SIN_CONTACTO`.
  - `COMPROMISO_PAGO` requiere fecha de compromiso (sin monto).
  - Geolocalización puntual **obligatoria**.
  - Fotografías opcionales (varias permitidas).
  - Observaciones de texto libre.
- Reporte de dirección corregida desde terreno.
- Sincronización automática al recuperar conectividad.
- Sincronización manual: "Sincronizar asignación" y "Enviar gestiones pendientes".
- Indicadores: modo offline, última sincronización, gestiones pendientes, errores.

### API central
- Autenticación y autorización por roles.
- Importación y validación de asignaciones (CSV).
- Exposición de asignación diaria para descarga al móvil.
- Recepción idempotente de gestiones (por UUID del dispositivo).
- Búsqueda global por RUT.
- Gestión de usuarios, carteras, asignaciones.
- Estado de dispositivos móviles.
- Contrato OpenAPI 3.

### Administración web
- Gestión de usuarios y roles.
- Importación de asignaciones mensuales por CSV.
- Preparación y envío de asignación diaria a ejecutivos.
- Visualización de gestiones registradas.
- Estado de dispositivos (última sync, versión, errores, revocación).
- Exportación a Excel (Fase 1 o posterior: **PENDIENTE**).

## Excluido explícitamente (primera etapa)

- Tracking continuo de ubicación de ejecutivos.
- Integración con sistema corporativo por API (solo CSV en Fase 1).
- Gestión de pagos o transacciones financieras directas.
- Generación automática de rutas de cobranza.
- Notificaciones push.
- Módulo de reportería avanzada.
- Almacenamiento S3 para fotografías (diseñado para ello, no implementado aún).
- Multiempresa o multitenant.
- Rectificación o anulación de gestiones.
- Soporte para XLSX en importaciones (evaluable si se requiere).

## PENDIENTE de definir

- Mecanismo de reapertura de la app estando offline (huella, PIN de app, u otro).
- Duración máxima de la sesión offline.
- Versión mínima de Android (requiere inventario de dispositivos corporativos).
- Si la exportación a Excel se incluye en Fase 1 o en una posterior.
- Qué operaciones se incluyen en la descarga: ¿todas o solo con cuotas vencidas?
