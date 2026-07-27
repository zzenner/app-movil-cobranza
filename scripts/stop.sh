#!/usr/bin/env bash
# Detiene los contenedores sin eliminar volúmenes de datos.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$ROOT_DIR"

echo "Deteniendo contenedores de cobranza (los volúmenes de datos se conservan)..."
docker compose down

echo "Contenedores detenidos."
