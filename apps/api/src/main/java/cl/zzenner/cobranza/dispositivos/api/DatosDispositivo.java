package cl.zzenner.cobranza.dispositivos.api;

import java.time.Instant;
import java.util.UUID;

public record DatosDispositivo(
        UUID id,
        UUID usuarioId,
        String identificadorInstalacion,
        String nombreDispositivo,
        String fabricante,
        String modelo,
        String versionAndroid,
        String versionAplicacion,
        boolean activo,
        boolean revocado,
        Instant fechaRegistro
) {}
