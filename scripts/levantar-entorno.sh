#!/usr/bin/env bash
# Levanta el entorno Docker local completo: PostgreSQL + API + admin web.
# Requisitos: .env configurado, claves RSA generadas (./scripts/generar-claves.sh).
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

# ── Verificar Docker ──────────────────────────────────────────────────────────
if ! docker info >/dev/null 2>&1; then
    echo "ERROR: Docker no está disponible o no tiene permisos."
    echo "En WSL2: verificar que Docker Desktop esté iniciado y la integración WSL2 activa."
    exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
    echo "ERROR: docker compose (plugin v2) no está disponible."
    exit 1
fi

# ── Verificar .env ────────────────────────────────────────────────────────────
if [ ! -f .env ]; then
    echo "ERROR: El archivo .env no existe."
    echo "Ejecutar: cp .env.example .env"
    echo "Luego configurar POSTGRES_PASSWORD y DEV_ADMIN_PASSWORD."
    exit 1
fi

# ── Verificar claves RSA ──────────────────────────────────────────────────────
if [ ! -f infrastructure/dev-keys/private.pem ] || [ ! -f infrastructure/dev-keys/public.pem ]; then
    echo "ERROR: Claves RSA no encontradas en infrastructure/dev-keys/."
    echo "Ejecutar primero: ./scripts/generar-claves.sh"
    exit 1
fi

echo "=== Levantando entorno Docker local de cobranza ==="
docker compose up --build -d

echo ""
echo "Esperando a que todos los servicios estén healthy..."
TIMEOUT=180
ELAPSED=0
INTERVAL=5
ALL_HEALTHY=false

while [ "$ELAPSED" -lt "$TIMEOUT" ]; do
    ALL_HEALTHY=true
    for svc in postgres api admin-web; do
        STATUS=$(docker compose ps --format "{{.Service}}:{{.Health}}" 2>/dev/null \
            | grep "^${svc}:" | cut -d: -f2 || echo "unknown")
        if [ "$STATUS" != "healthy" ]; then
            ALL_HEALTHY=false
            break
        fi
    done
    if $ALL_HEALTHY; then
        echo "Todos los servicios están healthy."
        break
    fi
    sleep "$INTERVAL"
    ELAPSED=$((ELAPSED + INTERVAL))
done

if ! $ALL_HEALTHY; then
    echo ""
    echo "ADVERTENCIA: tiempo de espera agotado. Estado actual:"
    docker compose ps
    echo ""
    echo "Para diagnóstico: docker compose logs api --tail=50"
    exit 1
fi

echo ""
echo "=== Entorno disponible ==="
echo "  Admin web: http://localhost:${ADMIN_WEB_PORT:-8080}"
echo "  API directa (Swagger): http://localhost:${API_PORT:-8081}/swagger-ui/index.html"
echo "  Actuator:  http://localhost:${API_PORT:-8081}/actuator/health"
echo ""
echo "Para smoke tests: ./scripts/smoke-test.sh"
echo "Para logs:        docker compose logs -f api"
echo "Para detener:     docker compose down"
echo "Para limpiar BD:  docker compose down -v"
