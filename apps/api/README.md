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
│   └── api/           — PersonaConsultaApi (@NamedInterface)
├── operaciones/       — copia operacional: créditos y cuotas por persona (Fase 3A ✅)
│   └── api/           — OperacionConsultaApi (@NamedInterface)
├── gestiones/         — recepción idempotente de gestiones de terreno (Fase 3C ✅)
│   └── api/           — GestionConsultaApi, ResultadoRecepcion (@NamedInterface)
├── sincronizacion/    — bundle descarga: personas + ops + gestiones (Fase 3D ✅)
├── auditoria/         — trazabilidad de operaciones críticas (stub)
└── compartido/        — utilidades transversales sin lógica de dominio (stub)
```

## Migraciones Flyway (V001–V010)

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

### Autenticación (públicos)

| Endpoint | Descripción |
|---|---|
| `POST /api/v1/auth/login` | Login con credenciales; retorna access token + refresh token |
| `POST /api/v1/auth/refresh` | Renovar access token con refresh token |
| `POST /api/v1/auth/logout` | Cerrar sesión (revoca refresh token) |
| `GET /api/v1/auth/me` | Perfil del usuario autenticado |

### Sincronización Android (rol `EJECUTIVO_TERRENO`)

| Endpoint | Respuestas | Descripción |
|---|---|---|
| `GET /api/v1/asignaciones/diaria/activa` | 200 / 204 | Descarga bundle completo de la asignación diaria activa. Parámetro `?fecha` opcional (ISO-8601). 204 si no hay asignación PUBLICADA. |
| `POST /api/v1/gestiones` | 201 / 200 / 409 | Recepción idempotente de gestión. 201 = nueva, 200 = idempotente, 409 = conflicto UUID. El ejecutivo se extrae del JWT (`sub`). |

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
