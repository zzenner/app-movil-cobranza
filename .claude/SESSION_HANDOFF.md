# Handoff de sesión — inicio Fase 4C

**Fecha:** 2026-08-02
**Rama activa:** `feature/fase-4c-gestiones-offline`

---

## Estado de Git

| Referencia | Hash | Descripción |
|---|---|---|
| `main` (local + origin) | `f9382a2` | feat(android): implementar descarga y consulta offline fase 4b |
| `origin/main` | `f9382a2` | Igual a main local |
| `tag v0.10.0-descarga-offline` | `f9382a2` | Fase 4B cerrada y etiquetada |
| `feature/fase-4c-gestiones-offline` | `f9382a2` | Base = main; sin commits propios aún |
| `feature/fase-4b-descarga-offline` | `f9382a2` | Cerrada y fusionada a main |

Árbol limpio. Sin cambios sin confirmar.

---

## Pruebas validadas — Fase 4B (resultado final)

| Suite | Resultado |
|---|---|
| API `./mvnw clean verify` | ✅ 248 tests — 0 failures — BUILD SUCCESS |
| Android JVM total | ✅ 97 tests — 0 failures |
| Android `assembleDebug` | ✅ BUILD SUCCESSFUL |
| Android `lint` | ✅ BUILD SUCCESSFUL — sin errores |
| Android `assembleDebugAndroidTest` | ✅ BUILD SUCCESSFUL |
| Android `connectedDebugAndroidTest` | ⏭️ Sin emulador en WSL2 |

### Desglose JVM

| Clase | Tests |
|---|---|
| `BigDecimalSerializerTest` | 8 |
| `AsignacionRepositoryTest` | 7 |
| `AsignacionViewModelTest` | 5 |
| `DescargaAsignacionWorkerTest` | 5 |
| `AsignacionMapperTest` | 6 |
| `DatabaseSchemaTest` | 4 |
| `BundleReplacementTransactionTest` | 6 |
| `PersonaDaoTest` | 5 |
| `SyncMetadataDaoTest` | 5 |
| `LogoutUseCaseTest` | 4 |
| `LogoutIntegrationTest` | 4 |
| Fase 4A (auth, network, security) | 38 |
| **Total** | **97** |

---

## Correcciones aplicadas en verificación final Fase 4B

1. `BundleReplacementTransaction.reemplazar()` — añadido `fechaConsultada = bundle.asignacion.fecha`
2. `SyncMetadataEntity` — `versionContrato` y `generadoEn` documentados como reservados
3. `LogoutUseCase.invoke()` — `runCatching { sessionRepository.logout() }` (best-effort)
4. `AsignacionDescargaRestTest` Test 13 — contrato JSON Android validado
5. `LogoutUseCaseTest` — 4 tests MockK
6. `LogoutIntegrationTest` — 4 tests Robolectric Room
7. `CobranzaDatabase` — `exportSchema = true`; schema `1.json` versionado

---

## Siguiente acción exacta

**Rama:** `feature/fase-4c-gestiones-offline`

**Acción:** Revisar documentación de gestiones, GPS, outbox y sincronización antes de proponer plan para Fase 4C.

Documentos a leer:
1. `docs/sincronizacion/PROTOCOLO_SINCRONIZACION.md`
2. `docs/adr/0026-dos-origenes-de-gestion.md`
3. `docs/adr/0027-*` a `docs/adr/0030-fotografias-diferidas.md`
4. ADR-0033 a ADR-0036 (decisiones técnicas Android Fase 4B)

**No implementar nada** hasta que el plan de Fase 4C esté aprobado.

---

## No repetir

- Commit de Fase 4B (en `f9382a2`)
- Push de `feature/fase-4b-descarga-offline` (publicada)
- Merge a main (fast-forward completado)
- Tag `v0.10.0-descarga-offline` (publicado)
- Crear rama `feature/fase-4c-gestiones-offline` (creada y publicada)
- Agregar `fechaConsultada` a `BundleReplacementTransaction` (corregido en 4B)
- Agregar Test 13 a `AsignacionDescargaRestTest` (corregido en 4B)
- Crear `LogoutUseCaseTest` (creado en 4B)
- Crear `LogoutIntegrationTest` Room/Robolectric (creado en 4B)
- Corregir best-effort en `LogoutUseCase` (corregido en 4B)
