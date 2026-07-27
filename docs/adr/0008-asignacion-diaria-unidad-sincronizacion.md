# ADR-0008 — Asignación diaria como unidad de descarga al móvil

## Estado
Aceptado.

## Contexto
El ejecutivo de terreno tiene una asignación mensual de ~300–400 personas. Descargar todas las personas al teléfono en cada sincronización sería costoso en ancho de banda, almacenamiento y tiempo. Se necesita definir cuántos datos descarga el teléfono y cuándo.

## Decisión
La unidad de descarga al teléfono es la **asignación diaria**: un subconjunto de ~50 personas seleccionado por la aplicación administrativa cada día desde la asignación mensual del ejecutivo. La app Android descarga exclusivamente la asignación diaria vigente y sus datos relacionados (operaciones, cuotas, gestiones históricas, direcciones, valores financieros).

## Consecuencias

**Positivas:**
- Volumen de datos manejable: ~50 personas × ~3 cuotas × últimas 10 gestiones = conjunto acotado y predecible.
- La asignación diaria es la base natural de la ruta de trabajo del cobrador.
- Reduce el tiempo de sincronización inicial y el uso de datos móviles.
- El historial de asignaciones diarias permite supervisión y auditoría del trabajo real enviado cada día.

**Negativas:**
- El ejecutivo no tiene acceso offline a toda su cartera mensual; solo a las personas del día.
- Si una persona del día no estaba en la asignación diaria, el ejecutivo no puede verla sin conexión.
- La aplicación administrativa debe tener la operación de "preparar asignación diaria" claramente definida en su flujo.

## Alternativas consideradas

**Descargar toda la asignación mensual:** Menos dependencia del proceso de preparación diaria, pero implica descargar ~300–400 personas con todos sus datos. Costoso y más lento. Se descartó para el MVP.

**Sin asignación previa (pull libre):** El ejecutivo descarga personas a demanda. Requiere conexión constante o una estrategia de caché más compleja. No se alinea con el flujo de trabajo planificado de cobranza. Se descartó.
