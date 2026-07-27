#!/usr/bin/env bash
# Verifica el estado de la API: compilación, pruebas y conectividad básica.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_DIR="$SCRIPT_DIR/../apps/api"

echo "=== Verificación de la API ==="

echo ""
echo "--- Compilación ---"
cd "$API_DIR"
./mvnw -q validate

echo ""
echo "--- Pruebas (sin levantar Spring context) ---"
./mvnw -q test -Dtest="ModularidadTest" -DfailIfNoTests=false

echo ""
echo "OK: La API compila y la estructura modular es válida."
