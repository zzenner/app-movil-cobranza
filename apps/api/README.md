# cobranza-api

API REST del sistema de cobranza en terreno.

## Stack

- Java 21 (OpenJDK / Temurin)
- Spring Boot 3.5.16
- Spring Modulith 1.4.12
- PostgreSQL 16 + PostGIS 3.4
- Flyway 11 (gestión de esquema)
- Springdoc OpenAPI 2.8.17

## Requisitos locales

- Java 21
- Docker Desktop (con integración WSL2 habilitada)

## Estructura de módulos

```
cl.zzenner.cobranza/
├── autenticacion/     — login, JWT RS256, refresh tokens, logout (Fase 2 ✅)
├── usuarios/          — usuarios, roles, permisos, supervisión (Fase 1C ✅)
│   └── api/           — UsuarioConsultaApi (@NamedInterface)
├── dispositivos/      — registro y revocación de dispositivos Android (Fase 1C ✅)
├── carteras/          — carteras de cobranza y relación N:M con personas (Fase 3A ✅)
├── asignaciones/      — asignaciones mensuales y diarias con estados (Fase 3B ✅)
│   └── api/           — AsignacionConsultaApi (@NamedInterface)
├── personas/          — copia operacional: personas, avales, direcciones (Fase 3A ✅)
│   └── api/           — PersonaConsultaApi, RutValidacionApi (@NamedInterface)
├── operaciones/       — copia operacional: créditos y cuotas por persona (Fase 3A ✅)
│   └── api/           — OperacionConsultaApi (@NamedInterface)
├── gestiones/         — recepción idempotente de gestiones de terreno (Fase 3C ✅)
│   └── api/           — GestionConsultaApi, ResultadoRecepcion (@NamedInterface)
├── sincronizacion/    — bundle descarga + búsqueda directa por RUT (Fases 3D + 4C-B ✅)
├── auditoria/         — trazabilidad de operaciones críticas (stub)
└── compartido/        — utilidades transversales sin lógica de dominio (stub)
```

## Migraciones Flyway (V001–V011)

| Migración | Contenido |
|---|---|
| V001 | Esquemas `cobranza` y `auditoria` |
| V002 | Tablas de usuarios, roles, permisos y dispositivos |
| V003 | Datos iniciales: 4 roles, 7 permisos |
| V004 | Sesiones de autenticación y refresh tokens |
| V005 | Columna `bloqueado_hasta` en usuarios |
| V006 | Configuración de sistema |
| V007 | Carteras y personas |
| V008 | Relación N:M `carteras_personas` con historial |
| V009 | Asignaciones mensuales y diarias |
| V010 | Gestiones de cobranza (append-only, idempotente) |
| V011 | `tipo_cliente` en sesiones (ANDROID/WEB); `dispositivo_id` nullable; CHECKs; índices parciales |

## Comandos de desarrollo

```bash
# Iniciar PostgreSQL
./scripts/start.sh

# Iniciar la API (carga .env automáticamente)
./scripts/api-run.sh

# Ejecutar solo prueba de estructura modular (rápido, sin Docker)
cd apps/api && ./mvnw test -Dtest="ModularidadTest"

# Ejecutar todas las pruebas (incluye Testcontainers, requiere Docker)
./scripts/api-test.sh

# Verificar compilación y módulos (rápido)
./scripts/api-check.sh
```

## Endpoints disponibles

### Autenticación Android (públicos)

| Endpoint | Descripción |
|---|---|
| `POST /api/v1/auth/login` | Login con credenciales + `identificadorInstalacion`; retorna access token + refresh token |
| `POST /api/v1/auth/refresh` | Renovar access token con refresh token (body) |
| `POST /api/v1/auth/logout` | Cerrar sesión Android (revoca refresh token) |
| `GET /api/v1/auth/me` | Perfil del usuario autenticado (ANDROID y WEB) |

### Autenticación Web — Panel administrativo (públicos excepto logout)

| Endpoint | Requiere | Descripción |
|---|---|---|
| `POST /api/v1/auth/web/login` | — | Login sin `identificadorInstalacion`. Access token en body; refresh token en cookie HttpOnly `rt_web`. |
| `POST /api/v1/auth/web/refresh` | Cookie `rt_web` + `Origin` | Rota el refresh token. Rechaza con 403 si Origin no coincide con `WEB_ALLOWED_ORIGIN`. |
| `POST /api/v1/auth/web/logout` | Bearer JWT + `Origin` | Revoca sesión. Limpia cookie `rt_web` (Max-Age=0). Rechaza con 403 si Origin no coincide. |

### Sincronización Android (rol `EJECUTIVO_TERRENO`)

| Endpoint | Respuestas | Descripción |
|---|---|---|
| `GET /api/v1/asignaciones/diaria/activa` | 200 / 204 | Descarga bundle completo de la asignación diaria activa. Parámetro `?fecha` opcional (ISO-8601). 204 si no hay asignación PUBLICADA. |
| `POST /api/v1/gestiones` | 201 / 200 / 409 | Recepción idempotente de gestión. 201 = nueva, 200 = idempotente, 409 = conflicto UUID. El ejecutivo se extrae del JWT (`sub`). |
| `POST /api/v1/personas/busquedas` | 200 / 400 / 404 | Búsqueda directa por RUT. RUT en body por privacidad (ADR-0041). 400 = RUT inválido, 404 = no existe. Respuesta `{ version, generadoEn, persona }`. |

### Administración de usuarios — Panel web (permiso `USUARIOS_VER`)

| Endpoint | Respuestas | Descripción |
|---|---|---|
| `GET /api/v1/admin/usuarios` | 200 / 400 / 401 / 403 | Listado paginado de usuarios. Filtros: `nombreUsuario` (parcial, insensible), `estado` (ACTIVO/BLOQUEADO_TEMPORAL/BLOQUEADO/INACTIVO), `rol`. Paginación: `pagina` (≥0), `tamanio` (1–50, default 20). Orden: nombreUsuario ASC. Solo lectura. |
| `GET /api/v1/admin/usuarios/{id}` | 200 / 400 / 401 / 403 / 404 | Detalle completo: roles vigentes, permisos efectivos, estado calculado, supervisor. No expone `contrasenaHash`. |

Acceso: `JEFE_SUPERVISORES` y `TECNOLOGIA`. `SUPERVISOR` y `EJECUTIVO_TERRENO` reciben 403. Ver ADR-0046.

### Infraestructura (públicos)

| Endpoint | Descripción |
|---|---|
| `GET /actuator/health` | Estado de la aplicación |
| `GET /actuator/health/liveness` | Liveness probe (Kubernetes) |
| `GET /actuator/health/readiness` | Readiness probe (Kubernetes) |
| `GET /v3/api-docs` | Documentación OpenAPI (JSON) |
| `GET /swagger-ui/index.html` | Interfaz Swagger UI |

## Configuración

La API usa dos archivos de configuración:
- `application.yml` — configuración base (sin datasource).
- `application-local.yml` — datasource local leído de variables de entorno.

Variables de entorno necesarias (del archivo `.env`):
- `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `WEB_ALLOWED_ORIGIN` — origen permitido para cookie web (default: `http://localhost:4200`)
- `WEB_COOKIE_SECURE` — marcar cookie `rt_web` como Secure (default: `false`, activar en producción HTTPS)
