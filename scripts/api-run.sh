#!/usr/bin/env bash
# Inicia la API en modo local cargando variables de entorno desde .env.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/../.env"
API_DIR="$SCRIPT_DIR/../apps/api"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: No se encontró el archivo .env en la raíz del proyecto."
  echo "       Copia .env.example a .env y configura los valores."
  exit 1
fi

# Exporta las variables del archivo .env sin imprimir sus valores
set -o allexport
# shellcheck disable=SC1090
source "$ENV_FILE"
set +o allexport

echo "=== Iniciando cobranza-api (perfil: local) ==="
cd "$API_DIR"
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
