# ADR-0002 — API como monolito modular (Spring Modulith)

## Estado
Aceptado.

## Contexto
Se necesita decidir la arquitectura de la API central: microservicios independientes versus un monolito. El equipo es pequeño, el sistema es nuevo y los límites exactos entre módulos de negocio todavía se están descubriendo.

## Decisión
Implementar la API como un monolito modular usando Spring Boot con Spring Modulith. Los módulos tienen fronteras explícitas de dominio (clases y paquetes) pero comparten un único proceso, despliegue y base de datos.

## Consecuencias

**Positivas:**
- Operativamente simple: un proceso, un `compose.yaml`, un único punto de despliegue.
- Spring Modulith hace cumplir las fronteras de módulo en tiempo de compilación y permite generar documentación de dependencias.
- Si los límites cambian, refactorizar módulos internos es mucho más barato que dividir microservicios.
- Una sola base de datos PostgreSQL simplifica las transacciones y la consistencia.

**Negativas:**
- Escalado horizontal escala todo el proceso, no módulos individuales.
- Un fallo crítico en un módulo puede afectar a los demás (riesgo mitigable con pruebas).
- Si en el futuro se requieren microservicios, hay un trabajo de extracción significativo.

## Alternativas consideradas

**Microservicios desde el inicio:** Mayor complejidad operacional (service mesh, descubrimiento de servicios, transacciones distribuidas) y overhead de desarrollo que no se justifica para el tamaño actual del equipo y del sistema.

**Monolito sin modularización:** Sin Spring Modulith, el riesgo de acoplamientos accidentales entre áreas de negocio es alto. Se descartó por mantenibilidad.
