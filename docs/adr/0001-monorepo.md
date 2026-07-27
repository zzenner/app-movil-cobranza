# ADR-0001 — Uso de monorepo Git

## Estado
Aceptado.

## Contexto
El sistema de cobranza en terreno tiene tres aplicaciones relacionadas: una API central, una administración web y una app Android. Estas tres aplicaciones comparten dominio, contratos OpenAPI y convenciones, y evolucionan en conjunto. Se necesita decidir si se usan repositorios separados (polyrepo) o un único repositorio (monorepo).

## Decisión
Usar un único repositorio Git (monorepo) para las tres aplicaciones y toda la documentación e infraestructura compartida.

## Consecuencias

**Positivas:**
- Los contratos OpenAPI, esquemas y ejemplos están en el mismo repositorio que los consumidores.
- Los cambios que afectan a varios componentes se pueden hacer en un solo commit o PR, garantizando consistencia.
- La documentación, ADR y convenciones son accesibles desde un único lugar.
- Facilita la revisión cruzada de cambios entre equipos.

**Negativas:**
- El repositorio crecerá en tamaño con el tiempo.
- Las pipelines de CI deben filtrar qué parte del repositorio cambió para ejecutar solo los builds relevantes.
- Requiere disciplina para no modificar módulos no relacionados en cada PR.

## Alternativas consideradas

**Polyrepo:** Un repositorio por componente. Ofrece independencia de despliegue y pipelines más simples por repo, pero genera complejidad en la gestión de versiones de contratos, más overhead operacional y dificultad para hacer cambios coordinados.

## Notas
No se usan submódulos Git (complejidad innecesaria en este caso).
