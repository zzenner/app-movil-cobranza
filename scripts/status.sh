#!/usr/bin/env bash
# Muestra el estado de los contenedores del entorno local.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$ROOT_DIR"

echo "Estado del entorno local de cobranza:"
echo ""
docker compose ps
