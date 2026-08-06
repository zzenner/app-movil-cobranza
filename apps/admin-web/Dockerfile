# Build context: apps/admin-web/
# En compose.yaml: context: apps/admin-web, dockerfile: Dockerfile

# ─── Etapa 1: build Angular ──────────────────────────────────────────────────
FROM node:24-alpine AS build
WORKDIR /app

# Copiar manifiestos primero para aprovechar caché de capas
COPY package.json package-lock.json ./

# Instalar dependencias de forma reproducible (sin scripts de postinstall externos)
RUN npm ci --ignore-scripts

# Copiar fuentes y compilar para producción
COPY . .
RUN npm run build

# ─── Etapa 2: runtime Nginx ───────────────────────────────────────────────────
FROM nginx:1.27-alpine AS runtime

# Copiar archivos estáticos compilados de Angular
COPY --from=build /app/dist/admin-web/browser /usr/share/nginx/html

# Copiar configuración de Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# Healthcheck HTTP
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://127.0.0.1/nginx-health || exit 1
