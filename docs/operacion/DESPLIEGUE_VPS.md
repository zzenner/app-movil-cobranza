# Despliegue en VPS

> **Estado: FUTURO — no implementado.**
>
> Este documento describe la estrategia prevista de despliegue en producción. No existe todavía infraestructura de producción. Se actualizará al iniciar la fase de despliegue.

## Arquitectura objetivo

```
Internet
   │
   ▼
Nginx (proxy inverso + TLS)
   ├── /api/*  → API Spring Boot (contenedor)
   └── /*      → Admin Web Angular (archivos estáticos)

API → PostgreSQL + PostGIS (contenedor)
API → S3 / MinIO (almacenamiento de fotografías, futuro)
```

## Requisitos del VPS

- Ubuntu Server 22.04 LTS o superior.
- Docker Engine instalado (no Docker Desktop).
- Docker Compose plugin.
- Nginx.
- Certificado TLS (Let's Encrypt recomendado).
- Al menos 2 GB de RAM y 20 GB de disco para comenzar.
- Acceso SSH con clave, sin contraseña.

## Stack de producción

| Componente      | Imagen / Herramienta           |
|-----------------|-------------------------------|
| Base de datos   | `postgis/postgis:16-3.4`      |
| API             | Imagen Docker propia (Fase 1) |
| Admin Web       | Nginx sirviendo dist Angular  |
| Proxy inverso   | Nginx                         |
| TLS             | Let's Encrypt + Certbot       |
| Orquestación    | Docker Compose                |

## TODO (fases futuras)

- [ ] Crear `compose.prod.yaml` con configuración de producción.
- [ ] Definir variables de entorno de producción y su gestión segura (no en Git).
- [ ] Configurar archivos de Nginx para proxy inverso y TLS.
- [ ] Definir estrategia de renovación automática de certificados TLS.
- [ ] Documentar proceso de deploy (pull, rebuild, restart con mínimo downtime).
- [ ] Definir monitoreo básico (uptime, logs, alertas).
- [ ] Definir política de actualizaciones de seguridad del SO.

## PENDIENTE de decidir

- Proveedor de VPS (DigitalOcean, Hetzner, Linode, propio, etc.).
- Nombre de dominio y configuración DNS.
- Estrategia de gestión de secretos en producción (variables de entorno del sistema, Docker Secrets, Vault, etc.).
- Estrategia de certificados TLS si el servidor no tiene acceso público directo (ACME DNS challenge).
- SLA y monitoreo de disponibilidad.
