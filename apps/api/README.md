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
├── autenticacion/     — login, JWT, logout
├── usuarios/          — usuarios y roles
├── dispositivos/      — registro y revocación de dispositivos Android
├── carteras/          — carteras de cobranza
├── asignaciones/      — asignaciones mensuales y diarias
├── personas/          — copia operacional de personas y datos financieros
├── operaciones/       — créditos y cuotas por persona
├── gestiones/         — registro idempotente de gestiones de terreno
├── sincronizacion/    — coordinación de sincronización con Android
├── auditoria/         — trazabilidad de operaciones críticas
└── compartido/        — utilidades transversales sin lógica de dominio
```

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

| Endpoint | Descripción |
|---|---|
| `GET /actuator/health` | Estado de la aplicación |
| `GET /actuator/health/liveness` | Liveness probe (Kubernetes) |
| `GET /actuator/health/readiness` | Readiness probe (Kubernetes) |
| `GET /actuator/info` | Información de la aplicación |
| `GET /v3/api-docs` | Documentación OpenAPI (JSON) |
| `GET /swagger-ui/index.html` | Interfaz Swagger UI |

## Configuración

La API usa dos archivos de configuración:
- `application.yml` — configuración base (sin datasource).
- `application-local.yml` — datasource local leído de variables de entorno.

Variables de entorno necesarias (del archivo `.env`):
- `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
