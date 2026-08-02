# ADR-0036 — Arquitectura del módulo `:feature:asignacion` (Fase 4B)

**Estado:** Aceptado — 2026-08-02
**Contexto:** Fase 4B — Cartera offline

---

## Contexto

La Fase 4B introduce la pantalla principal de la cartera del ejecutivo. Debe decidirse cómo organizar la navegación multi-módulo, el alcance de Hilt para el repositorio, y cómo manejar el búfer de estado en el ViewModel con flujos reactivos de Room.

## Decisiones

### 1. Navegación: el NavHost vive en `:app`

El `NavHost` principal se mueve desde `:feature:auth` a `:app`. Cada módulo expone una función de extensión sobre `NavGraphBuilder`:

```kotlin
// feature:auth
fun NavGraphBuilder.authNavGraph(navController, onLoginExitoso)

// feature:asignacion
fun NavGraphBuilder.asignacionNavGraph(navController, onLogout)
```

`:app` compone los grafos sin importar módulos feature-to-feature:

```kotlin
NavHost(startDestination = "auth") {
    authNavGraph(navController) { navController.navigate("home") }
    composable("home") { HomeScreen(onIrACartera = { navController.navigate("asignacion/lista") }) }
    asignacionNavGraph(navController) { onLogout() }
}
```

**Consecuencia:** los módulos feature no se conocen entre sí; solo `:app` conoce a todos.

### 2. Alcance de `SessionRepository`: `@Singleton`

En la Fase 4A, `SessionRepository` era `@ActivityRetainedScoped`. Al introducir `AsignacionRepository (@Singleton)`, este necesita acceder a `TokenProvider` (que `SessionRepository` implementa). Un `@Singleton` no puede depender de un `@ActivityRetainedScoped`.

**Cambio:** `SessionRepository` pasa a `@Singleton`. Se añade `AuthModule` en `:feature:auth` para enlazar `SessionRepository` como `TokenProvider` con `@Binds @Singleton`.

**Consecuencia:** `SessionRepository` vive mientras vive la aplicación. Los tokens en memoria persisten entre recomposiciones, lo que es correcto.

### 3. `AsignacionViewModel` — `stateIn` con `WhileSubscribed`

El estado combinado de la pantalla usa `combine(personas, metadata, busqueda).stateIn(WhileSubscribed(5_000))`. Con `WhileSubscribed(5_000)`, el upstream se pausa 5 segundos después de que no hay suscriptores (ej. la pantalla pasa a background), lo que evita queries innecesarias.

El ViewModel combina tres fuentes:
- `observePersonasDeAsignacion()` — `Flow<List<PersonaConDetalle>>` de Room
- `observeSyncMetadata()` — `Flow<SyncMetadataEntity?>` de Room
- `_textoBusqueda` — `MutableStateFlow<String>`

El filtrado de personas por RUT/nombre se realiza en el ViewModel, no en el DAO, para mantener la query Room simple y reutilizable.

### 4. Single-flight en `AsignacionRepository`

`AsignacionRepository.descargarAsignacion()` usa un `Mutex` interno para garantizar que solo haya una descarga en vuelo al mismo tiempo, incluso si el Worker y el usuario invocan la descarga simultáneamente.

```kotlin
private val mutex = Mutex()

suspend fun descargarAsignacion(): ResultadoDescarga = mutex.withLock {
    ...
}
```

### 5. `LogoutUseCase` en `:app`

El caso de uso de logout orquesta tres módulos que no deben conocerse entre sí:
- `SessionRepository` (`:feature:auth`)
- `AsignacionSyncScheduler` (`:feature:asignacion`)
- `BundleReplacementTransaction` (`:core:database`)

Colocar esta lógica en `:app` evita dependencias circulares entre módulos feature.

## Consecuencias

- El grafo de dependencias Gradle queda: `:app` → `:feature:auth`, `:feature:asignacion`; `:feature:asignacion` → `:core:database`, `:core:network`; `:feature:auth` → `:core:network`, `:core:security`.
- Los módulos feature no dependen entre sí.
- La navigación type-safe entre features pasa siempre por `:app`.
- El cambio de `@ActivityRetainedScoped` a `@Singleton` en `SessionRepository` es no regresivo: el comportamiento observable desde las pantallas no cambia.

## Alternativas descartadas

- **NavHost en `:feature:auth`:** introducía dependencia de `:feature:auth` hacia `:feature:asignacion`, generando un ciclo.
- **Mantener `@ActivityRetainedScoped` con módulo auxiliar:** más complejo sin beneficio real.
- **Filtrado de personas en el DAO (SQL LIKE):** requeriría reiniciar el Flow completo de Room en cada keystroke del buscador; el filtro en ViewModel con `combine` es más eficiente para listas pequeñas (~50 personas).
