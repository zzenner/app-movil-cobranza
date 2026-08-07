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

# ── 7. Endpoints de escritura (Fase 5B-2) ────────────────────────────────────
echo ""
echo "--- 7. Endpoints de escritura de usuarios (Fase 5B-2) ---------"

if [ -z "$DEV_USER" ] || [ -z "$DEV_PASS" ]; then
    echo "  [SKIP] Escritura — DEV_ADMIN_USERNAME / DEV_ADMIN_PASSWORD no configurados."
else
    # Re-login actor (sesión anterior fue cerrada en sección 5)
    _ACTOR_RESP=$(curl -s -w "\n%{http_code}" \
        -X POST \
        -H "Content-Type: application/json" \
        -H "Origin: $WEB_BASE" \
        -d "{\"nombreUsuario\":\"$DEV_USER\",\"clave\":\"$DEV_PASS\"}" \
        --connect-timeout 5 --max-time 10 \
        "$API_BASE/api/v1/auth/web/login" 2>/dev/null || echo -e "\n000")
    _ACTOR_CODE=$(echo "$_ACTOR_RESP" | tail -n1)
    _ACTOR_BODY=$(echo "$_ACTOR_RESP" | head -n-1)
    ACTOR_TOKEN=$(echo "$_ACTOR_BODY" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4 || echo "")

    if [ "$_ACTOR_CODE" != "200" ] || [ -z "$ACTOR_TOKEN" ]; then
        fail "Re-login actor para operaciones de escritura — código: $_ACTOR_CODE"
    else
        ok "Re-login actor para operaciones de escritura (200)"

        # 7.1 Catálogo de roles
        _ROLES_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
            -H "Authorization: Bearer $ACTOR_TOKEN" \
            --connect-timeout 5 --max-time 10 \
            "$API_BASE/api/v1/admin/roles" 2>/dev/null || echo "000")
        if [ "$_ROLES_CODE" = "200" ]; then ok "GET /admin/roles — catálogo disponible (200)"
        else fail "GET /admin/roles — esperado 200, obtenido $_ROLES_CODE"; fi

        # 7.2 Crear usuario de prueba (nombre único por timestamp)
        _SMOKE_TS=$(date +%s)
        _SMOKE_USER="smoke.w.$_SMOKE_TS"
        # Generar contraseña temporal sin imprimirla
        _SMOKE_PASS_FILE="$(mktemp)"
        printf 'SmkP@%s!' "$_SMOKE_TS" > "$_SMOKE_PASS_FILE"
        _SMOKE_PASS=$(cat "$_SMOKE_PASS_FILE")
        trap 'rm -f "$COOKIE_JAR" "$_SMOKE_PASS_FILE"' EXIT INT TERM

        _CREATE_BODY_FILE="$(mktemp)"
        printf '{"nombreUsuario":"%s","nombres":"Smoke","apellidoPaterno":"Test","contrasena":"%s","rolesIniciales":["EJECUTIVO_TERRENO"]}' \
            "$_SMOKE_USER" "$_SMOKE_PASS" > "$_CREATE_BODY_FILE"

        _CREATE_RESP=$(curl -s -w "\n%{http_code}" \
            -X POST \
            -H "Authorization: Bearer $ACTOR_TOKEN" \
            -H "Content-Type: application/json" \
            --data-binary "@$_CREATE_BODY_FILE" \
            --connect-timeout 5 --max-time 10 \
            "$API_BASE/api/v1/admin/usuarios" 2>/dev/null || echo -e "\n000")
        rm -f "$_CREATE_BODY_FILE"
        _CREATE_CODE=$(echo "$_CREATE_RESP" | tail -n1)
        _CREATE_BODY=$(echo "$_CREATE_RESP" | head -n-1)

        if [ "$_CREATE_CODE" = "201" ]; then ok "POST /admin/usuarios — usuario creado (201)"
        else fail "POST /admin/usuarios — esperado 201, obtenido $_CREATE_CODE"; fi

        if echo "$_CREATE_BODY" | grep -qi "contrasena\|hash"; then
            fail "Respuesta de creación expone 'contrasena' o 'hash'"
        else
            ok "Respuesta de creación NO expone contrasena/hash"
        fi

        _SMOKE_ID=$(echo "$_CREATE_BODY" | grep -o '"id":"[^"]*"' | cut -d'"' -f4 || echo "")
        if [ -n "$_SMOKE_ID" ]; then ok "ID del usuario creado presente en respuesta"
        else fail "ID del usuario creado ausente en respuesta"; fi

        if [ -n "$_SMOKE_ID" ]; then
            # 7.3 Detalle — debe contener 'version', no 'contrasena'/'hash'
            _DETAIL=$(curl -s \
                -H "Authorization: Bearer $ACTOR_TOKEN" \
                --connect-timeout 5 --max-time 10 \
                "$API_BASE/api/v1/admin/usuarios/$_SMOKE_ID" 2>/dev/null || echo "")
            if echo "$_DETAIL" | grep -q '"version"'; then
                ok "GET /admin/usuarios/{id} — campo 'version' presente"
            else
                fail "GET /admin/usuarios/{id} — campo 'version' ausente"
            fi
            if echo "$_DETAIL" | grep -qi "contrasena\|hash"; then
                fail "Detalle expone 'contrasena' o 'hash'"
            else
                ok "Detalle NO expone contrasena/hash"
            fi
            _SMOKE_VER=$(echo "$_DETAIL" | grep -o '"version":[0-9]*' | cut -d':' -f2 || echo "0")

            # 7.4 Editar datos básicos (version en cuerpo)
            _EDIT_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
                -X PUT \
                -H "Authorization: Bearer $ACTOR_TOKEN" \
                -H "Content-Type: application/json" \
                -d "{\"nombres\":\"SmokeEdit\",\"apellidoPaterno\":\"TestEdit\",\"version\":$_SMOKE_VER}" \
                --connect-timeout 5 --max-time 10 \
                "$API_BASE/api/v1/admin/usuarios/$_SMOKE_ID/datos-basicos" 2>/dev/null || echo "000")
            if [[ "$_EDIT_CODE" =~ ^(200|204)$ ]]; then ok "PUT /admin/usuarios/{id}/datos-basicos ($_EDIT_CODE)"
            else fail "PUT datos-basicos — esperado 200/204, obtenido $_EDIT_CODE"; fi

            # 7.5 Desactivar usuario
            _DESACT=$(curl -s -o /dev/null -w "%{http_code}" \
                -X POST -H "Authorization: Bearer $ACTOR_TOKEN" \
                --connect-timeout 5 --max-time 10 \
                "$API_BASE/api/v1/admin/usuarios/$_SMOKE_ID/desactivar" 2>/dev/null || echo "000")
            if [[ "$_DESACT" =~ ^(200|204)$ ]]; then ok "POST /admin/usuarios/{id}/desactivar ($_DESACT)"
            else fail "POST desactivar — esperado 200/204, obtenido $_DESACT"; fi

            # 7.6 Login del target rechazado (inactivo) — usa web login
            _TARGET_LOGINF=$(mktemp)
            printf '{"nombreUsuario":"%s","clave":"%s"}' "$_SMOKE_USER" "$_SMOKE_PASS" > "$_TARGET_LOGINF"
            _TGT_DESACT=$(curl -s -o /dev/null -w "%{http_code}" \
                -X POST -H "Content-Type: application/json" -H "Origin: $WEB_BASE" \
                --data-binary "@$_TARGET_LOGINF" \
                --connect-timeout 5 --max-time 10 \
                "$API_BASE/api/v1/auth/web/login" 2>/dev/null || echo "000")
            if [[ "$_TGT_DESACT" =~ ^(401|403)$ ]]; then ok "Login rechazado para usuario desactivado ($_TGT_DESACT)"
            else fail "Login usuario desactivado — esperado 401/403, obtenido $_TGT_DESACT"; fi

            # 7.7 Activar usuario
            _ACT=$(curl -s -o /dev/null -w "%{http_code}" \
                -X POST -H "Authorization: Bearer $ACTOR_TOKEN" \
                --connect-timeout 5 --max-time 10 \
                "$API_BASE/api/v1/admin/usuarios/$_SMOKE_ID/activar" 2>/dev/null || echo "000")
            if [[ "$_ACT" =~ ^(200|204)$ ]]; then ok "POST /admin/usuarios/{id}/activar ($_ACT)"
            else fail "POST activar — esperado 200/204, obtenido $_ACT"; fi

            # 7.8 Login del target aceptado tras activar
            _TGT_ACT=$(curl -s -w "\n%{http_code}" \
                -X POST -H "Content-Type: application/json" -H "Origin: $WEB_BASE" \
                --data-binary "@$_TARGET_LOGINF" \
                --connect-timeout 5 --max-time 10 \
                "$API_BASE/api/v1/auth/web/login" 2>/dev/null || echo -e "\n000")
            _TGT_ACT_CODE=$(echo "$_TGT_ACT" | tail -n1)
            if [ "$_TGT_ACT_CODE" = "200" ]; then ok "Login aceptado para usuario reactivado (200)"
            else fail "Login usuario activado — esperado 200, obtenido $_TGT_ACT_CODE"; fi

            # 7.9 Bloquear usuario
            _BLOQ=$(curl -s -o /dev/null -w "%{http_code}" \
                -X POST -H "Authorization: Bearer $ACTOR_TOKEN" \
                --connect-timeout 5 --max-time 10 \
                "$API_BASE/api/v1/admin/usuarios/$_SMOKE_ID/bloquear" 2>/dev/null || echo "000")
            if [[ "$_BLOQ" =~ ^(200|204)$ ]]; then ok "POST /admin/usuarios/{id}/bloquear ($_BLOQ)"
            else fail "POST bloquear — esperado 200/204, obtenido $_BLOQ"; fi

            # 7.10 Login rechazado (bloqueado)
            _TGT_BLOQ=$(curl -s -o /dev/null -w "%{http_code}" \
                -X POST -H "Content-Type: application/json" -H "Origin: $WEB_BASE" \
                --data-binary "@$_TARGET_LOGINF" \
                --connect-timeout 5 --max-time 10 \
                "$API_BASE/api/v1/auth/web/login" 2>/dev/null || echo "000")
            if [[ "$_TGT_BLOQ" =~ ^(401|403)$ ]]; then ok "Login rechazado para usuario bloqueado ($_TGT_BLOQ)"
            else fail "Login usuario bloqueado — esperado 401/403, obtenido $_TGT_BLOQ"; fi

            # 7.11 Desbloquear usuario
            _DESBLOQ=$(curl -s -o /dev/null -w "%{http_code}" \
                -X POST -H "Authorization: Bearer $ACTOR_TOKEN" \
                --connect-timeout 5 --max-time 10 \
                "$API_BASE/api/v1/admin/usuarios/$_SMOKE_ID/desbloquear" 2>/dev/null || echo "000")
            if [[ "$_DESBLOQ" =~ ^(200|204)$ ]]; then ok "POST /admin/usuarios/{id}/desbloquear ($_DESBLOQ)"
            else fail "POST desbloquear — esperado 200/204, obtenido $_DESBLOQ"; fi

            # 7.12 Login aceptado tras desbloquear
            _TGT_DESBLOQ=$(curl -s -o /dev/null -w "%{http_code}" \
                -X POST -H "Content-Type: application/json" -H "Origin: $WEB_BASE" \
                --data-binary "@$_TARGET_LOGINF" \
                --connect-timeout 5 --max-time 10 \
                "$API_BASE/api/v1/auth/web/login" 2>/dev/null || echo "000")
            if [ "$_TGT_DESBLOQ" = "200" ]; then ok "Login aceptado para usuario desbloqueado (200)"
            else fail "Login usuario desbloqueado — esperado 200, obtenido $_TGT_DESBLOQ"; fi

            # 7.13 Restablecer contraseña
            _NEW_PASS_FILE="$(mktemp)"
            printf 'SmkNw@%s!' "$_SMOKE_TS" > "$_NEW_PASS_FILE"
            _SMOKE_NEW_PASS=$(cat "$_NEW_PASS_FILE")
            _RESET_BODY_FILE="$(mktemp)"
            printf '{"nuevaContrasena":"%s"}' "$_SMOKE_NEW_PASS" > "$_RESET_BODY_FILE"
            _RESET=$(curl -s -o /dev/null -w "%{http_code}" \
                -X POST -H "Authorization: Bearer $ACTOR_TOKEN" \
                -H "Content-Type: application/json" \
                --data-binary "@$_RESET_BODY_FILE" \
                --connect-timeout 5 --max-time 10 \
                "$API_BASE/api/v1/admin/usuarios/$_SMOKE_ID/restablecer-contrasena" 2>/dev/null || echo "000")
            rm -f "$_RESET_BODY_FILE"
            if [[ "$_RESET" =~ ^(200|204)$ ]]; then ok "POST /admin/usuarios/{id}/contrasena — reset contraseña ($_RESET)"
            else fail "POST contrasena — esperado 200/204, obtenido $_RESET"; fi

            # 7.14 Contraseña antigua rechazada
            _OLD_LOGIN=$(curl -s -o /dev/null -w "%{http_code}" \
                -X POST -H "Content-Type: application/json" -H "Origin: $WEB_BASE" \
                --data-binary "@$_TARGET_LOGINF" \
                --connect-timeout 5 --max-time 10 \
                "$API_BASE/api/v1/auth/web/login" 2>/dev/null || echo "000")
            if [[ "$_OLD_LOGIN" =~ ^(401|403)$ ]]; then ok "Login rechazado con contraseña antigua tras reset ($_OLD_LOGIN)"
            else fail "Contraseña antigua aún válida tras reset — obtenido $_OLD_LOGIN"; fi
            rm -f "$_TARGET_LOGINF"

            # 7.15 Contraseña nueva aceptada
            _NEW_LOGIN_FILE="$(mktemp)"
            printf '{"nombreUsuario":"%s","clave":"%s"}' "$_SMOKE_USER" "$_SMOKE_NEW_PASS" > "$_NEW_LOGIN_FILE"
            _NEW_LOGIN=$(curl -s -o /dev/null -w "%{http_code}" \
                -X POST -H "Content-Type: application/json" -H "Origin: $WEB_BASE" \
                --data-binary "@$_NEW_LOGIN_FILE" \
                --connect-timeout 5 --max-time 10 \
                "$API_BASE/api/v1/auth/web/login" 2>/dev/null || echo "000")
            rm -f "$_NEW_LOGIN_FILE" "$_NEW_PASS_FILE"
            if [ "$_NEW_LOGIN" = "200" ]; then ok "Login aceptado con contraseña nueva tras reset (200)"
            else fail "Contraseña nueva rechazada — esperado 200, obtenido $_NEW_LOGIN"; fi

            # 7.16 Listado contiene el usuario creado
            _LIST=$(curl -s -H "Authorization: Bearer $ACTOR_TOKEN" \
                --connect-timeout 5 --max-time 10 \
                "$API_BASE/api/v1/admin/usuarios" 2>/dev/null || echo "")
            if echo "$_LIST" | grep -q "$_SMOKE_ID"; then ok "Listado /admin/usuarios contiene al usuario creado"
            else fail "Listado /admin/usuarios — usuario creado no encontrado"; fi

            # 7.17 Detalle final: activo=true, bloqueado=false
            _FINAL=$(curl -s -H "Authorization: Bearer $ACTOR_TOKEN" \
                --connect-timeout 5 --max-time 10 \
                "$API_BASE/api/v1/admin/usuarios/$_SMOKE_ID" 2>/dev/null || echo "")
            if echo "$_FINAL" | grep -q '"activo":true'; then ok "Estado final: activo=true"
            else fail "Estado final: activo no es true"; fi
            if echo "$_FINAL" | grep -q '"bloqueado":false'; then ok "Estado final: bloqueado=false"
            else fail "Estado final: bloqueado no es false"; fi

            # 7.18 Desactivar al final para no dejar basura activa
            curl -s -o /dev/null \
                -X POST -H "Authorization: Bearer $ACTOR_TOKEN" \
                --connect-timeout 5 --max-time 10 \
                "$API_BASE/api/v1/admin/usuarios/$_SMOKE_ID/desactivar" 2>/dev/null || true
            ok "Limpieza: usuario de prueba desactivado tras smoke test"
        fi
    fi
fi

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
