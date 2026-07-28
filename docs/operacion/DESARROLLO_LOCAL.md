# Desarrollo local

## Requisitos previos

- Docker Desktop con soporte WSL2 (Windows) o Docker Engine (Linux/macOS).
- Git.
- Para la API: JDK 21 (recomendado: OpenJDK o Temurin).
- Para la app Android: Android Studio con SDK 34+.
- Para la web admin: Node.js 20+ y Angular CLI.

Verificar Docker:
```bash
docker --version
docker compose version
```

## Configuración inicial

```bash
# 1. Clonar el repositorio
git clone https://github.com/zzenner/app-movil-cobranza.git
cd app-movil-cobranza

# 2. Crear archivo de entorno local
cp .env.example .env
# Editar .env con valores locales si es necesario
# (los valores por defecto del .env.example sirven para desarrollo)

# 3. Validar la configuración
./scripts/check.sh
```

## Iniciar la base de datos

```bash
# Iniciar PostgreSQL + PostGIS
./scripts/start.sh

# Ver estado
./scripts/status.sh

# Ver logs
./scripts/logs.sh postgres

# Iniciar también Adminer (interfaz web para la base de datos)
docker compose --profile tools up -d
```

Adminer estará disponible en `http://localhost:8080`.
- **Sistema:** PostgreSQL
- **Servidor:** `postgres`
- **Usuario:** el valor de `POSTGRES_USER` en `.env`
- **Contraseña:** el valor de `POSTGRES_PASSWORD` en `.env`
- **Base de datos:** el valor de `POSTGRES_DB` en `.env`

## Verificar la conexión a PostgreSQL

```bash
docker compose exec postgres psql \
  -U "$POSTGRES_USER" \
  -d "$POSTGRES_DB" \
  -c "SELECT PostGIS_Full_Version();"
```

## Detener el entorno

```bash
# Detener sin eliminar datos
./scripts/stop.sh

# Limpiar contenedores (conserva datos)
./scripts/clean.sh

# Limpiar contenedores Y volúmenes (elimina datos locales)
./scripts/clean.sh --volumes
```

## Estructura de desarrollo por componente

**API (`apps/api/`):**

```bash
# Iniciar la API con perfil local (carga .env automáticamente)
./scripts/api-run.sh

# Ejecutar todas las pruebas (requiere Docker)
./scripts/api-test.sh

# Verificar compilación y estructura modular (no requiere Docker)
./scripts/api-check.sh
```

La API inicia en `http://localhost:8080`. Endpoints disponibles:
- `GET /actuator/health` — estado de la aplicación
- `GET /v3/api-docs` — documentación OpenAPI (JSON)
- `GET /swagger-ui/index.html` — Swagger UI

**App Android (`apps/mobile-android/`):** PENDIENTE — instrucciones se agregarán al iniciar la Fase 3.

**Admin Web (`apps/admin-web/`):** PENDIENTE — instrucciones se agregarán al iniciar la Fase 4.

## Puertos por defecto

| Servicio   | Puerto local | Variable de entorno |
|------------|-------------|---------------------|
| PostgreSQL | 5432        | `POSTGRES_PORT`     |
| API        | 8080        | — (fijo en Fase 1B) |
| Adminer    | 8081        | `ADMINER_PORT`      |

## Resolución de problemas comunes

**El puerto 5432 ya está en uso:**
Cambiar `POSTGRES_PORT` en `.env` por otro puerto libre, por ejemplo `5433`.

**El healthcheck de PostgreSQL falla:**
Verificar que los valores de `POSTGRES_USER` y `POSTGRES_DB` en `.env` coinciden con los que PostgreSQL intentó crear al inicializar. Si el volumen ya existía con otra configuración, puede requerirse limpiar el volumen:
```bash
./scripts/clean.sh --volumes
./scripts/start.sh
```

**Docker no tiene permisos en WSL2:**
Asegurarse de que Docker Desktop está corriendo y de que la integración con WSL2 está habilitada en Docker Desktop > Settings > Resources > WSL Integration.
