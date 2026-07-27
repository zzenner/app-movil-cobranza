# ADR-0003 — App Android nativa con Kotlin y Jetpack Compose

## Estado
Aceptado.

## Contexto
La app de campo debe operar offline de forma confiable en dispositivos Android, capturar geolocalización, tomar fotografías y sincronizarse en segundo plano. Se necesita decidir entre desarrollo nativo Android o una solución multiplataforma (Flutter, React Native, Kotlin Multiplatform).

## Decisión
Desarrollar la app Android con Kotlin nativo y Jetpack Compose. La base de datos local será Room (sobre SQLite) y la sincronización se gestionará con WorkManager.

## Consecuencias

**Positivas:**
- Acceso completo al ecosistema Android: WorkManager con garantías de ejecución, Keystore para almacenamiento seguro, Android Keystore para cifrado, APIs de geolocalización y cámara sin abstracción.
- Room integra perfectamente con Kotlin Coroutines y Flow para UI reactiva.
- Jetpack Compose es el estándar actual de UI en Android.
- Mejor rendimiento y mayor control sobre la experiencia offline.

**Negativas:**
- Solo aplica para Android; si en el futuro se requiere iOS, se necesita una app separada.
- El desarrollo nativo requiere experiencia específica en Android/Kotlin.

## Alternativas consideradas

**Flutter:** Soporta iOS y Android desde un único código. Se descartó porque el acceso a APIs nativas offline (WorkManager, Keystore) requiere plugins de terceros que añaden incertidumbre y complejidad, y porque no hay requisito de iOS en esta etapa.

**React Native:** Misma razón que Flutter. Además, el ecosistema de pruebas instrumentadas es más maduro en Android nativo.

**Kotlin Multiplatform:** Interesante para compartir lógica de negocio, pero la UI sigue siendo nativa y la madurez del ecosistema KMP para las APIs requeridas está en evolución.
