#!/bin/sh
# Prepara las claves RSA copiándolas con permisos mínimos y cede la ejecución a appuser.
# Ejecuta como root para poder leer el volumen independientemente del UID del host.
set -e

KEY_DIR="/run/cobranza-keys"
mkdir -p "$KEY_DIR"

if [ -f /keys/private.pem ]; then
    install -m 400 -o appuser -g appgroup /keys/private.pem "$KEY_DIR/private.pem"
    install -m 444 -o appuser -g appgroup /keys/public.pem  "$KEY_DIR/public.pem"
    export JWT_RSA_PRIVADA_PATH="$KEY_DIR/private.pem"
    export JWT_RSA_PUBLICA_PATH="$KEY_DIR/public.pem"
fi

exec su-exec appuser java $JAVA_OPTS -jar /app/app.jar
