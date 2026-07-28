/**
 * Módulo de dispositivos.
 *
 * <p>Responsabilidad: registro, activación y revocación de dispositivos Android.
 * Depende únicamente de la API pública del módulo usuarios ({@code usuarios::api}).
 *
 * <p>Estado: implementado en Fase 1C.
 */
@ApplicationModule(
        displayName = "Dispositivos",
        allowedDependencies = "usuarios::api"
)
package cl.zzenner.cobranza.dispositivos;

import org.springframework.modulith.ApplicationModule;
