#!/usr/bin/env bash
# Ejecuta todas las pruebas de la API incluyendo las de integración con Testcontainers.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_DIR="$SCRIPT_DIR/../apps/api"

echo "=== Pruebas completas de la API (incluye Testcontainers) ==="
echo "    Docker debe estar disponible."
echo ""

cd "$API_DIR"
./mvnw verify

echo ""
echo "OK: Todas las pruebas pasaron."
