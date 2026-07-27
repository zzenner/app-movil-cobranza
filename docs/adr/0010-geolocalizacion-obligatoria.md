# ADR-0010 — Geolocalización puntual obligatoria en gestiones

## Estado
Aceptado.

## Contexto
Las gestiones se realizan en terreno y la ubicación es información valiosa para verificar que el cobrador estuvo en el lugar correcto. Se necesita definir si la geolocalización es opcional u obligatoria, y qué nivel de precisión se acepta.

## Decisión
La geolocalización puntual es **obligatoria** para guardar una gestión. Sin coordenadas disponibles, la app no permite guardar. Si el GPS está desactivado, tampoco se permite registrar. Si el GPS está activo pero con precisión baja, se permite registrar y se almacena la precisión real.

Se almacenan: latitud, longitud, precisión en metros, fecha de captura, proveedor de ubicación y si la ubicación es simulada (mock location).

No se implementa tracking continuo ni almacenamiento de recorridos.

## Consecuencias

**Positivas:**
- Cada gestión tiene evidencia geográfica de dónde se realizó.
- El indicador de ubicación simulada (`mock_location`) permite detectar posible fraude.
- La información de precisión permite evaluar la confiabilidad del dato geoespacial.
- PostGIS almacena los puntos como GEOMETRY, habilitando consultas geoespaciales futuras.

**Negativas:**
- Si el ejecutivo está en una zona sin señal GPS (interior de edificios, zonas remotas sin cobertura de red), no puede registrar la gestión.
- Requiere que el ejecutivo tenga activado el GPS en el teléfono; si lo olvida, bloquea el registro.
- Añade latencia al registro de gestión si el GPS no tiene fix inmediato.

## Alternativas consideradas

**Geolocalización opcional:** Permite registrar sin GPS. Más flexible, pero pierde el valor probatorio de la ubicación. Se descartó porque la localización en terreno es un requisito del negocio.

**Geolocalización obligatoria con precisión mínima exigida:** Más estricto; rechaza registros con precisión mayor a X metros. Se evaluará en el futuro si se detectan abusos con registros de baja precisión. No se implementa en el MVP para no bloquear casos legítimos en zonas de señal débil.

**Tracking continuo:** Graba la trayectoria del ejecutivo durante el día. Se descartó explícitamente por privacidad y por no ser un requisito de la primera etapa.
