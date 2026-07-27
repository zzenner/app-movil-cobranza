#!/usr/bin/env bash
# Inicia el entorno local de desarrollo (base de datos).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$ROOT_DIR"

if [ ! -f .env ]; then
  echo "ERROR: El archivo .env no existe."
  echo "Copiar .env.example a .env y configurar los valores locales:"
  echo "  cp .env.example .env"
  exit 1
fi

echo "Iniciando entorno local de cobranza..."
docker compose up -d postgres

echo ""
echo "Esperando a que PostgreSQL esté listo..."
RETRIES=30
WAIT=2
for i in $(seq 1 $RETRIES); do
  if docker compose exec -T postgres pg_isready -q 2>/dev/null; then
    echo "PostgreSQL disponible."
    echo ""
    echo "Entorno iniciado. Para ver el estado: ./scripts/status.sh"
    exit 0
  fi
  echo "  Intento $i/$RETRIES — esperando ${WAIT}s..."
  sleep $WAIT
done

echo "ERROR: PostgreSQL no respondió en el tiempo esperado."
echo "Ver logs con: ./scripts/logs.sh"
exit 1
