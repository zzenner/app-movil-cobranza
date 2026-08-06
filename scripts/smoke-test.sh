#!/usr/bin/env bash
# Smoke tests automatizados — entorno Docker local.
# Las credenciales se leen exclusivamente desde variables de entorno.
# No se imprime access token, refresh token ni cookie.
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

# Cargar variables desde .env si existe
if [ -f .env ]; then
    set -o allexport
    # shellcheck disable=SC1091
    source .env
    set +o allexport
fi

API_PORT="${API_PORT:-8081}"
ADMIN_WEB_PORT="${ADMIN_WEB_PORT:-8080}"
API_BASE="http://localhost:${API_PORT}"
WEB_BASE="http://localhost:${ADMIN_WEB_PORT}"

# Archivo temporal seguro para cookies (eliminado con trap)
COOKIE_JAR="$(mktemp)"
trap 'rm -f "$COOKIE_JAR"' EXIT INT TERM

PASS=0
FAIL=0

ok()   { echo "  [OK]  $1"; PASS=$((PASS + 1)); }
fail() { echo "  [FAIL] $1"; FAIL=$((FAIL + 1)); }

check_http() {
    local label="$1" url="$2" expected="${3:-200}"
    local actual
    actual=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 10 "$url" 2>/dev/null || echo "000")
    if [ "$actual" = "$expected" ]; then ok "$label ($actual)"
    else fail "$label — esperado $expected, obtenido $actual — URL: $url"
    fi
}

check_body() {
    local label="$1" url="$2" pattern="$3"
    local body
    body=$(curl -s --connect-timeout 5 --max-time 10 "$url" 2>/dev/null || echo "")
    if echo "$body" | grep -q "$pattern"; then ok "$label"
    else fail "$label — patrón '$pattern' no encontrado en $url"
    fi
}

echo "======================================================"
echo " Smoke tests — Entorno Docker local de cobranza"
echo "======================================================"
echo " API:       $API_BASE"
echo " Admin web: $WEB_BASE"
echo ""

# ── 1. Infraestructura ────────────────────────────────────────────────────────
echo "--- 1. Infraestructura ----------------------------------------"

check_http "Nginx health check"                  "$WEB_BASE/nginx-health"
check_http "Actuator /health"                    "$API_BASE/actuator/health"
check_body "Actuator estado UP"                  "$API_BASE/actuator/health" '"status":"UP"'
check_http "Actuator /health/readiness"          "$API_BASE/actuator/health/readiness"
check_http "Actuator /health/liveness"           "$API_BASE/actuator/health/liveness"

# ── 2. API pública ────────────────────────────────────────────────────────────
echo ""
echo "--- 2. API pública --------------------------------------------"

check_http "OpenAPI JSON disponible"             "$API_BASE/v3/api-docs"
check_http "Swagger UI disponible"               "$API_BASE/swagger-ui/index.html"

# ── 3. SPA Angular (Nginx) ────────────────────────────────────────────────────
echo ""
echo "--- 3. SPA Angular (Nginx) ------------------------------------"

check_http "Raíz del SPA"                        "$WEB_BASE/"
check_http "Ruta /login devuelve index.html"     "$WEB_BASE/login"
check_http "Ruta /dashboard devuelve index.html" "$WEB_BASE/dashboard"

# Confirmar que Nginx devuelve SPA (no listado de directorio)
SPA_BODY=$(curl -s --connect-timeout 5 --max-time 10 "$WEB_BASE/cualquier-ruta-spa" 2>/dev/null || echo "")
if echo "$SPA_BODY" | grep -qi "<app-root"; then
    ok "SPA fallback entrega index.html con <app-root>"
else
    fail "SPA fallback — no se encontró <app-root> en respuesta de ruta arbitraria"
fi

# ── 4. Proxy /api → API a través de Nginx ────────────────────────────────────
echo ""
echo "--- 4. Proxy Nginx → API --------------------------------------"

PROXY_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Origin: $WEB_BASE" \
    --connect-timeout 5 --max-time 10 \
    "$WEB_BASE/api/v1/admin/usuarios" 2>/dev/null || echo "000")
if [[ "$PROXY_CODE" =~ ^(401|403)$ ]]; then
    ok "Proxy /api operativo — respuesta auth esperada ($PROXY_CODE)"
else
    fail "Proxy /api — código inesperado: $PROXY_CODE (esperado 401 o 403)"
fi

# El proxy /api/* → Spring — verificar que no hay error 5xx (502/504 = proxy roto)
# Un 401/403 es correcto: nginx llegó a Spring y Spring respondió
PROXY_ANY=$(curl -s -o /dev/null -w "%{http_code}" \
    --connect-timeout 5 --max-time 10 \
    "$WEB_BASE/api/v1/auth/me" 2>/dev/null || echo "000")
if [[ "$PROXY_ANY" =~ ^[123456789][0-9][0-9]$ ]] && ! [[ "$PROXY_ANY" =~ ^5 ]]; then
    ok "Proxy /api → Spring funcional (respuesta: $PROXY_ANY, sin 5xx)"
else
    fail "Proxy /api → error 5xx o sin respuesta ($PROXY_ANY)"
fi

# ── 5. Flujo de autenticación ─────────────────────────────────────────────────
echo ""
echo "--- 5. Flujo de autenticación ---------------------------------"

DEV_USER="${DEV_ADMIN_USERNAME:-}"
DEV_PASS="${DEV_ADMIN_PASSWORD:-}"

if [ -z "$DEV_USER" ] || [ -z "$DEV_PASS" ]; then
    echo "  [SKIP] Login — DEV_ADMIN_USERNAME / DEV_ADMIN_PASSWORD no configurados."
else
    # Login (cookies guardadas en COOKIE_JAR — no se imprimen)
    LOGIN_HTTP=$(curl -s -w "\n%{http_code}" \
        -c "$COOKIE_JAR" \
        -X POST \
        -H "Content-Type: application/json" \
        -H "Origin: $WEB_BASE" \
        -d "{\"nombreUsuario\":\"$DEV_USER\",\"clave\":\"$DEV_PASS\"}" \
        --connect-timeout 5 --max-time 10 \
        "$API_BASE/api/v1/auth/web/login" 2>/dev/null || echo -e "\n000")
    LOGIN_CODE=$(echo "$LOGIN_HTTP" | tail -n1)
    LOGIN_BODY=$(echo "$LOGIN_HTTP" | head -n-1)

    if [ "$LOGIN_CODE" = "200" ]; then
        ok "Login ($LOGIN_CODE)"
    else
        fail "Login — código inesperado: $LOGIN_CODE"
    fi

    # Access token presente (sin imprimirlo)
    ACCESS_TOKEN=$(echo "$LOGIN_BODY" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4 || echo "")
    if [ -n "$ACCESS_TOKEN" ]; then
        ok "accessToken presente en respuesta de login"
    else
        fail "accessToken ausente en respuesta de login"
        ACCESS_TOKEN=""
    fi

    # Verificar que refresh token NO aparece en el JSON (está en cookie)
    if echo "$LOGIN_BODY" | grep -qi "refreshToken\|refresh_token"; then
        fail "refresh token expuesto en cuerpo JSON de login (debería estar solo en cookie)"
    else
        ok "Refresh token no expuesto en JSON de login (cookie HttpOnly)"
    fi

    if [ -n "$ACCESS_TOKEN" ]; then
        # GET /auth/me
        ME_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
            -H "Authorization: Bearer $ACCESS_TOKEN" \
            --connect-timeout 5 --max-time 10 \
            "$API_BASE/api/v1/auth/me" 2>/dev/null || echo "000")
        if [ "$ME_CODE" = "200" ]; then
            ok "GET /auth/me con token válido ($ME_CODE)"
        else
            fail "GET /auth/me — esperado 200, obtenido $ME_CODE"
        fi

        # GET /admin/usuarios (requiere PERM_USUARIOS_VER — TECNOLOGIA lo tiene)
        USUARIOS_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
            -H "Authorization: Bearer $ACCESS_TOKEN" \
            --connect-timeout 5 --max-time 10 \
            "$API_BASE/api/v1/admin/usuarios" 2>/dev/null || echo "000")
        if [[ "$USUARIOS_CODE" =~ ^(200|403)$ ]]; then
            ok "GET /admin/usuarios con token ($USUARIOS_CODE)"
        else
            fail "GET /admin/usuarios — código inesperado: $USUARIOS_CODE"
        fi

        # Refresh token via Nginx (same-origin — usa cookie del COOKIE_JAR)
        REFRESH_HTTP=$(curl -s -w "\n%{http_code}" \
            -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
            -X POST \
            -H "Origin: $WEB_BASE" \
            -H "Referer: $WEB_BASE/" \
            --connect-timeout 5 --max-time 10 \
            "$WEB_BASE/api/v1/auth/web/refresh" 2>/dev/null || echo -e "\n000")
        REFRESH_CODE=$(echo "$REFRESH_HTTP" | tail -n1)
        REFRESH_BODY=$(echo "$REFRESH_HTTP" | head -n-1)

        if [ "$REFRESH_CODE" = "200" ]; then
            ok "POST /auth/web/refresh vía Nginx ($REFRESH_CODE)"
            NEW_TOKEN=$(echo "$REFRESH_BODY" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4 || echo "")
            if [ -n "$NEW_TOKEN" ]; then
                ok "Nuevo accessToken recibido tras refresh"
                ACCESS_TOKEN="$NEW_TOKEN"
            else
                fail "Nuevo accessToken ausente en respuesta de refresh"
            fi
        else
            fail "Refresh vía Nginx — código inesperado: $REFRESH_CODE"
        fi

        # Logout
        LOGOUT_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
            -b "$COOKIE_JAR" \
            -X POST \
            -H "Authorization: Bearer $ACCESS_TOKEN" \
            -H "Origin: $WEB_BASE" \
            -H "Referer: $WEB_BASE/" \
            --connect-timeout 5 --max-time 10 \
            "$API_BASE/api/v1/auth/web/logout" 2>/dev/null || echo "000")
        if [[ "$LOGOUT_CODE" =~ ^(200|204)$ ]]; then
            ok "POST /auth/web/logout ($LOGOUT_CODE)"
        else
            fail "Logout — código inesperado: $LOGOUT_CODE"
        fi

        # JWT es stateless: el access token sigue siendo técnicamente válido hasta expirar.
        # Verificar que el REFRESH TOKEN queda invalidado en servidor tras logout.
        POST_LOGOUT_REFRESH=$(curl -s -o /dev/null -w "%{http_code}" \
            -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
            -X POST \
            -H "Origin: $WEB_BASE" \
            -H "Referer: $WEB_BASE/" \
            --connect-timeout 5 --max-time 10 \
            "$WEB_BASE/api/v1/auth/web/refresh" 2>/dev/null || echo "000")
        if [[ "$POST_LOGOUT_REFRESH" =~ ^(401|400)$ ]]; then
            ok "Refresh token invalidado en BD tras logout ($POST_LOGOUT_REFRESH)"
        else
            fail "Refresh aún válido tras logout — código: $POST_LOGOUT_REFRESH (esperado 401/400)"
        fi
    fi
fi

# ── 6. Usuario no root ────────────────────────────────────────────────────────
echo ""
echo "--- 6. Seguridad de proceso -----------------------------------"

# El proceso Java (PID 1) debe correr como usuario no root
API_PROC_USER=$(docker compose exec api ps -o user= -p 1 2>/dev/null | tr -d '[:space:]' || echo "unknown")
if [ -n "$API_PROC_USER" ] && [ "$API_PROC_USER" != "root" ] && [ "$API_PROC_USER" != "unknown" ]; then
    ok "Proceso Java (PID 1) no corre como root (usuario: $API_PROC_USER)"
else
    # ps -o user puede no estar disponible — verificar via /proc
    PROC_UID=$(docker compose exec api cat /proc/1/status 2>/dev/null | grep '^Uid:' | awk '{print $2}' || echo "0")
    if [ "$PROC_UID" != "0" ]; then
        ok "Proceso Java (PID 1) no corre como root (UID: $PROC_UID)"
    else
        fail "Proceso Java corre como root (UID 0)"
    fi
fi

# La API healthy + JWT funcional = claves RSA leídas correctamente al inicio
# (el CargadorClavesRsa falla en startup si no puede leer la clave)
check_body "Claves RSA operativas (API healthy)" "$API_BASE/actuator/health/readiness" '"status":"UP"'

# ── Resultado ─────────────────────────────────────────────────────────────────
echo ""
echo "======================================================"
printf " Resultados: %d OK, %d FALLIDO\n" "$PASS" "$FAIL"
echo "======================================================"

if [ "$FAIL" -gt 0 ]; then
    echo ""
    echo "Para diagnóstico:"
    echo "  docker compose ps"
    echo "  docker compose logs api --tail=50"
    echo "  docker compose logs admin-web --tail=20"
    exit 1
fi

echo ""
echo "Todos los smoke tests pasaron."
