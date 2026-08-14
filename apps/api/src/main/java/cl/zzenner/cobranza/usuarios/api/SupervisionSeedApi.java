package cl.zzenner.cobranza.usuarios.api;

import java.util.UUID;

/**
 * Puerto público del módulo usuarios para asignación de supervisión en datos de desarrollo.
 * Solo para uso del DevSeedRunner bajo el perfil docker.
 */
public interface SupervisionSeedApi {

    /**
     * Asigna idempotentemente un ejecutivo a un supervisor.
     * Si el ejecutivo ya tiene ese supervisor activo, no realiza ninguna acción.
     */
    void asignarEjecutivoSiNoAsignado(UUID supervisorId, UUID ejecutivoId);
}
