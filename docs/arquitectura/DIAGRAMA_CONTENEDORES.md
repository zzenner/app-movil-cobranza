# Diagrama de contenedores

Diagrama C4 de nivel 2 (contenedores) del sistema de cobranza en terreno.

```mermaid
C4Container
    title Sistema de Cobranza en Terreno — Diagrama de Contenedores

    Person(cobrador, "Cobrador", "Profesional de cobranza que opera en terreno con dispositivo Android")
    Person(admin, "Administrador", "Gestiona usuarios, carteras y asignaciones desde la web")

    System_Boundary(cobranza, "Plataforma de Cobranza") {

        Container(android, "App Android", "Kotlin, Jetpack Compose, Room", "Permite registrar gestiones en terreno con soporte offline")
        Container(web, "Admin Web", "Angular (standalone)", "Interfaz de administración para usuarios, carteras y supervisión")
        Container(api, "API Central", "Java 21, Spring Boot, Spring Modulith", "Expone REST API, gestiona datos financieros y gestiones")
        ContainerDb(postgres, "Base de Datos", "PostgreSQL 16 + PostGIS 3.4", "Almacena todos los datos del sistema con soporte geoespacial")

    }

    System_Ext(storage, "Almacenamiento de Archivos", "Compatible con S3 (no implementado en Fase 1)")

    Rel(cobrador, android, "Registra gestiones y consulta cartera", "Pantalla táctil")
    Rel(admin, web, "Administra usuarios y carteras", "HTTPS / Navegador")

    Rel(android, api, "Sincroniza gestiones y descarga cartera", "HTTPS / JSON / REST")
    Rel(web, api, "Consulta y administra datos", "HTTPS / JSON / REST")
    Rel(api, postgres, "Lee y escribe datos", "JDBC / SQL")
    Rel(api, storage, "Almacena fotografías (futuro)", "HTTPS / S3 API")
```

## Notas del diagrama

- **App Android:** opera en modo offline-first. Room actúa como fuente de verdad local para la interfaz. La sincronización con la API ocurre en segundo plano mediante WorkManager.
- **API Central:** monolito modular. No hay microservicios. Un único proceso Spring Boot.
- **Admin Web:** sin soporte offline. Siempre requiere conexión a la API.
- **Almacenamiento de archivos:** el diseño contempla compatibilidad S3 para fotografías, pero no se implementa en la Fase 1.

## Infraestructura de despliegue (futuro)

```mermaid
graph TD
    Internet -->|HTTPS| Nginx
    Nginx -->|proxy_pass| API
    Nginx -->|static files| AdminWeb[Admin Web]
    API -->|JDBC| Postgres[(PostgreSQL)]
    API -.->|futuro| S3[(S3 / MinIO)]
```

El despliegue en producción se realizará en un VPS Ubuntu con Docker Compose y Nginx como proxy inverso. Este diagrama es preliminar y se actualizará en la fase de despliegue.
