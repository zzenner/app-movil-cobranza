#!/usr/bin/env bash
# Muestra los logs del entorno local. Opcionalmente filtrar por servicio.
# Uso: ./scripts/logs.sh [servicio]
# Ejemplo: ./scripts/logs.sh postgres
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$ROOT_DIR"

SERVICE="${1:-}"

if [ -n "$SERVICE" ]; then
  docker compose logs -f "$SERVICE"
else
  docker compose logs -f
fi
