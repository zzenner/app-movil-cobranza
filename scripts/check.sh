#!/usr/bin/env bash
# Valida la sintaxis del archivo Docker Compose y la existencia de .env.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$ROOT_DIR"

ERRORS=0

echo "=== Validación del entorno local ==="
echo ""

# Verificar que Docker está disponible
if ! command -v docker &>/dev/null; then
  echo "ERROR: Docker no está disponible en el PATH."
  ERRORS=$((ERRORS + 1))
else
  echo "OK  Docker disponible: $(docker --version)"
fi

# Verificar que Docker Compose está disponible
if ! docker compose version &>/dev/null; then
  echo "ERROR: 'docker compose' no está disponible."
  ERRORS=$((ERRORS + 1))
else
  echo "OK  Docker Compose disponible: $(docker compose version --short)"
fi

# Verificar existencia de .env
if [ ! -f .env ]; then
  echo "WARN  El archivo .env no existe. Usar .env.example como base."
else
  echo "OK  Archivo .env presente."
fi

# Validar sintaxis de compose.yaml
echo ""
echo "Validando compose.yaml..."
if docker compose config --quiet 2>&1; then
  echo "OK  compose.yaml es válido."
else
  echo "ERROR: compose.yaml contiene errores."
  ERRORS=$((ERRORS + 1))
fi

echo ""
if [ $ERRORS -eq 0 ]; then
  echo "Validación completada sin errores."
  exit 0
else
  echo "Validación completada con $ERRORS error(es)."
  exit 1
fi
