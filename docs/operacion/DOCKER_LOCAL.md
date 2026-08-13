# Entorno Docker local completo

Este documento describe cómo levantar la plataforma completa (PostgreSQL/PostGIS + API Spring Boot + Admin Web Angular/Nginx) en Docker para pruebas manuales locales.

Para el modo de desarrollo sin Docker (API en proceso, base de datos en contenedor), consultar [DESARROLLO_LOCAL.md](DESARROLLO_LOCAL.md).

## Requisitos

- Docker Engine 25+ o Docker Desktop con soporte WSL2
- `openssl` disponible en el PATH (para generar claves RSA)
- Bash (disponible en WSL2 en Windows)

## Inicio rápido

```bash
# 1. Configurar entorno
cp .env.example .env
# Editar .env: establecer contraseñas reales para POSTGRES_PASSWORD, DEV_ADMIN_PASSWORD

# 2. Generar claves RSA de desarrollo (solo la primera vez)
./scripts/generar-claves.sh

# 3. Levantar todo
./scripts/levantar-entorno.sh

# 4. Verificar que funciona
./scripts/smoke-test.sh
```

## Arquitectura del entorno Docker

```
Navegador
   │
   ▼  :8080 (ADMIN_WEB_PORT)
┌─────────────────────┐
│  Nginx 1.27-alpine  │  admin-web
│  (SPA Angular)      │
│                     │
│  /api/* → proxy     │──────────────────────────────────────┐
└─────────────────────┘                                      │
                                                             ▼  :8080 (interno)
                                              ┌─────────────────────────┐
                                              │  Spring Boot 3.5 (API)  │  api
                                              │  perfil: docker          │
                                              │  :8081 expuesto al host  │
                                              └─────────────────────────┘
                                                             │
                                                             ▼  :5432 (POSTGRES_PORT)
                                              ┌─────────────────────────┐
                                              │  PostgreSQL 16 + PostGIS│  postgres
                                              └─────────────────────────┘
```

## Servicios y puertos por defecto

| Servicio    | Puerto host       | Variable             | Descripción                           |
|-------------|-------------------|----------------------|---------------------------------------|
| Admin Web   | 8080              | `ADMIN_WEB_PORT`     | Punto de entrada principal (navegador)|
| API directa | 8081              | `API_PORT`           | Swagger UI, Actuator, acceso directo  |
| PostgreSQL  | 5432              | `POSTGRES_PORT`      | Base de datos                         |
| Adminer     | 8082 (opcional)   | `ADMINER_PORT`       | UI de base de datos (perfil tools)    |

## Configuración del archivo `.env`

Copiar `.env.example` a `.env` y ajustar:

```dotenv
POSTGRES_PASSWORD=una_contraseña_segura
DEV_SEED_ENABLED=true
DEV_ADMIN_USERNAME=admin.local
DEV_ADMIN_PASSWORD=otra_contraseña_segura
DEV_ADMIN_EMAIL=admin.local@dev.cl
DEV_ADMIN_ROL=TECNOLOGIA
```

El origen permitido `WEB_ALLOWED_ORIGIN` debe coincidir con el puerto de `ADMIN_WEB_PORT` (por defecto `http://localhost:8080`).

## Claves RSA

La API usa un par de claves RSA 2048-bit para firmar JWT. El script las genera en `infrastructure/dev-keys/` (excluido de Git).

```bash
./scripts/generar-claves.sh
```

Las claves son idempotentes: si ya existen, el script termina sin regenerarlas. Para forzar regeneración eliminar los archivos y ejecutar de nuevo.

**Importante:** estas claves son solo para desarrollo local. En producción se usan claves gestionadas de forma segura fuera del repositorio.

## Usuario de desarrollo (DevSeed)

Con `DEV_SEED_ENABLED=true`, la API crea automáticamente un usuario administrador al iniciar:

- Nombre de usuario: `DEV_ADMIN_USERNAME`
- Contraseña: `DEV_ADMIN_PASSWORD`
- Rol: `DEV_ADMIN_ROL` (por defecto `TECNOLOGIA`)

La creación es idempotente: si el usuario ya existe, se omite sin error.

Para desactivar el seed: `DEV_SEED_ENABLED=false` en `.env`.

## Comandos de operación

```bash
# Levantar (build + start)
./scripts/levantar-entorno.sh

# O directamente con docker compose
docker compose up --build -d

# Ver estado de los servicios
docker compose ps

# Logs en tiempo real
docker compose logs -f api
docker compose logs -f admin-web

# Detener sin borrar datos
docker compose down

# Detener y borrar volúmenes (datos de BD)
docker compose down -v

# Reconstruir imagen de la API
docker compose build api

# Reconstruir imagen del admin web
docker compose build admin-web

# Levantar con Adminer (perfil tools)
docker compose --profile tools up -d
```

### Reconstruir imágenes tras cambios de código

Si modificaste el código fuente (API Java o Angular) y el entorno ya está levantado, reconstruye solo la imagen afectada:

```bash
# Solo API
docker compose build api && docker compose up -d api

# Solo admin-web
docker compose build admin-web && docker compose up -d admin-web

# Ambas (con espera de healthcheck)
docker compose build api admin-web && docker compose up -d
```

Los contenedores `api` y `admin-web` se recrean automáticamente manteniendo PostgreSQL activo.

## Smoke tests automatizados

```bash
./scripts/smoke-test.sh
```

El script verifica:
1. Nginx health check (`/nginx-health`)
2. Actuator `/health`, `/health/readiness`, `/health/liveness`
3. OpenAPI JSON y Swagger UI
4. SPA Angular (rutas devuelven index.html)
5. Proxy `/api` → API (espera 401/403, no 502/504)
6. Flujo de autenticación completo (login, /me, /admin/usuarios)

## Conectividad con el emulador Android

El emulador Android no puede usar `localhost` para apuntar al host. La dirección especial del emulador es:

| Dirección | A quién apunta |
|-----------|----------------|
| `10.0.2.2` | Host de la máquina de desarrollo (localhost del host) |
| `localhost` | Propio emulador (no el host) |

**URL correcta de la API desde el emulador:** `http://10.0.2.2:8081/`

La URL se define en `apps/mobile-android/core/network/build.gradle.kts` (campo `BuildConfig.BASE_URL`).

**Para verificar desde el emulador:**
1. Iniciar Docker Compose: `./scripts/levantar-entorno.sh`
2. Verificar en el host: `curl http://localhost:8081/actuator/health`
3. Abrir Chrome en el emulador y navegar a: `http://10.0.2.2:8081/actuator/health`
4. Debe responder `{"status":"UP", ...}`

**Credenciales de prueba (solo entorno Docker dev):**
- Usuario: valor de `DEV_ADMIN_USERNAME` en `.env` (por defecto `admin.local`)
- Contraseña: valor de `DEV_ADMIN_PASSWORD` en `.env`
- Rol: `TECNOLOGIA`

**Nota:** `DevSeedRunner` usa creación idempotente (`crearSiNoExiste`). Si el usuario ya existe en la base de datos, cambiar `DEV_ADMIN_PASSWORD` en `.env` no actualiza la contraseña almacenada. Para forzar re-creación: `docker compose down -v && ./scripts/levantar-entorno.sh`.

**Seguridad HTTP local:**
El build `debug` de Android incluye `src/debug/res/xml/network_security_config.xml` que permite tráfico HTTP (cleartext) solo a `10.0.2.2` y `localhost`. El build `release` usa `src/main/res/xml/network_security_config.xml` que rechaza todo cleartext.

## Checklist de prueba manual

- [ ] `http://localhost:8080` carga la pantalla de login
- [ ] Login con `DEV_ADMIN_USERNAME` / `DEV_ADMIN_PASSWORD`
- [ ] Redirección post-login al dashboard
- [ ] Navegación a `/usuarios` lista usuarios (o mensaje vacío)
- [ ] Botón de logout cierra la sesión
- [ ] `http://localhost:8081/swagger-ui/index.html` carga Swagger UI
- [ ] `http://localhost:8081/actuator/health` devuelve `{"status":"UP"}`

**Checklist Android (emulador):**
- [ ] Docker Compose levantado y `api` healthy
- [ ] Chrome en emulador: `http://10.0.2.2:8081/actuator/health` → `{"status":"UP"}`
- [ ] App Android debug: login con `DEV_ADMIN_USERNAME` / `DEV_ADMIN_PASSWORD` exitoso
- [ ] Estado `Autenticado` alcanzado (sin error "Sin conexión" ni "Error en el servidor")

## Limpieza y ciclo completo

Para verificar el entorno desde cero (sin datos persistidos):

```bash
docker compose down -v
./scripts/levantar-entorno.sh
./scripts/smoke-test.sh
```

## Resolución de problemas

**El puerto 8080 ya está en uso:**
Cambiar `ADMIN_WEB_PORT` en `.env` por otro puerto libre (ej. `4200`).

**La API no levanta (healthcheck falla):**
```bash
docker compose logs api --tail=100
```
Causas comunes: claves RSA ausentes, `POSTGRES_PASSWORD` no configurado, error de Flyway.

**El proxy `/api` devuelve 502:**
Verificar que el servicio `api` está healthy:
```bash
docker compose ps
```
La configuración de `depends_on.api.condition: service_healthy` en el compose garantiza que `admin-web` no se inicia hasta que la API esté lista. Si `admin-web` ya inició antes que la API, reiniciar: `docker compose restart admin-web`.

**La cookie de refresh no funciona:**
La cookie `rt_web` es HttpOnly y SameSite=Strict. El refresh debe hacerse desde el mismo origen que el login. En el entorno Docker, el navegador accede por `http://localhost:8080` y el proxy Nginx reenvía las peticiones de refresh. Verificar que `WEB_ALLOWED_ORIGIN=http://localhost:8080` en `.env`.

**Docker en WSL2 no tiene acceso de red:**
Verificar que Docker Desktop tiene la integración WSL2 activada en Settings > Resources > WSL Integration.
