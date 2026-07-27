#!/usr/bin/env bash
# Detiene y elimina los contenedores SIN eliminar los volúmenes de datos.
# Para eliminar también los volúmenes, usar la opción --volumes.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$ROOT_DIR"

REMOVE_VOLUMES=false
if [ "${1:-}" = "--volumes" ]; then
  REMOVE_VOLUMES=true
fi

if $REMOVE_VOLUMES; then
  echo "ADVERTENCIA: Se eliminarán los contenedores Y los volúmenes de datos."
  echo "Los datos de PostgreSQL se perderán."
  read -rp "¿Continuar? [s/N] " CONFIRM
  if [[ "$CONFIRM" != "s" && "$CONFIRM" != "S" ]]; then
    echo "Operación cancelada."
    exit 0
  fi
  docker compose down -v
  echo "Contenedores y volúmenes eliminados."
else
  echo "Eliminando contenedores (los volúmenes de datos se conservan)..."
  docker compose down
  echo "Contenedores eliminados. Los datos persisten en el volumen Docker."
fi
