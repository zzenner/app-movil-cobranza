#!/usr/bin/env bash
# Genera un par de claves RSA 2048-bit para uso local de desarrollo.
# Las claves se guardan en infrastructure/dev-keys/ (excluido de Git).
# Ejecutar UNA SOLA VEZ antes de levantar el entorno Docker.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
KEYS_DIR="$ROOT_DIR/infrastructure/dev-keys"

mkdir -p "$KEYS_DIR"

if [[ -f "$KEYS_DIR/private.pem" && -f "$KEYS_DIR/public.pem" ]]; then
    echo "Las claves ya existen en $KEYS_DIR — no se regeneran."
    echo "Para forzar regeneración: eliminar los archivos y volver a ejecutar."
    exit 0
fi

echo "Generando par RSA 2048-bit en $KEYS_DIR..."

# Clave privada PKCS8 (formato que espera CargadorClavesRsa.java)
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
    -out "$KEYS_DIR/private.pem" 2>/dev/null

# Clave pública en formato X.509 (SubjectPublicKeyInfo)
openssl rsa -pubout -in "$KEYS_DIR/private.pem" \
    -out "$KEYS_DIR/public.pem" 2>/dev/null

chmod 600 "$KEYS_DIR/private.pem"
chmod 644 "$KEYS_DIR/public.pem"

echo "Claves generadas:"
echo "  Privada: $KEYS_DIR/private.pem"
echo "  Pública: $KEYS_DIR/public.pem"
echo ""
echo "IMPORTANTE: estas claves son solo para desarrollo local."
echo "No versionar. Están excluidas de Git por .gitignore (*.pem)."
