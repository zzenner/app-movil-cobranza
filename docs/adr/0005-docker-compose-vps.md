# ADR-0005 — Docker Compose en VPS como estrategia de despliegue inicial

## Estado
Aceptado.

## Contexto
Se necesita definir la estrategia de despliegue de producción para la plataforma. Las opciones van desde un VPS simple hasta Kubernetes en la nube.

## Decisión
Desplegar la plataforma en un VPS Ubuntu con Docker Compose y Nginx como proxy inverso. No se usa Kubernetes ni servicios administrados de nube en la primera etapa.

## Consecuencias

**Positivas:**
- Operativamente simple: un servidor, Docker Compose, Nginx. Sin control planes ni abstracciones adicionales.
- El mismo `compose.yaml` (con ajustes) se usa en desarrollo local y en producción, lo que reduce la distancia entre entornos.
- Costo operacional bajo (un VPS es significativamente más barato que clusters Kubernetes o servicios administrados).
- El equipo puede administrar el sistema sin necesitar experiencia profunda en Kubernetes.

**Negativas:**
- Escalabilidad limitada: un solo servidor. Si el tráfico crece significativamente, se requiere migrar.
- Sin alta disponibilidad automática. Un fallo del servidor implica downtime.
- Las actualizaciones requieren downtime breve (estrategia de rolling update no disponible sin Kubernetes).

## Alternativas consideradas

**Kubernetes (autoprovisionado o GKE/EKS/AKS):** Mucha complejidad operacional para el tamaño actual del sistema y del equipo. Se evaluará en el futuro si el sistema escala.

**Plataformas PaaS (Railway, Render, Fly.io):** Reducen operación pero añaden dependencia de proveedor y pueden ser más costosas a escala. Se evaluará si el VPS propio genera demasiada carga operacional.

## Notas
Esta decisión es reversible: la arquitectura de contenedores Docker permite migrar a Kubernetes o a plataformas PaaS en el futuro con esfuerzo moderado.
