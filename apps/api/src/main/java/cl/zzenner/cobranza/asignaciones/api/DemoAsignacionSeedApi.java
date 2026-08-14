package cl.zzenner.cobranza.asignaciones.api;

import java.util.UUID;

/**
 * Puerto público del módulo asignaciones para preparar el escenario demo.
 * Solo para uso del DevSeedRunner bajo el perfil docker.
 *
 * Crea idempotentemente:
 *  - 1 asignacion_mensual demo para el período actual y la cartera indicada
 *  - 5 personas asociadas a esa mensual
 *  - 1 asignacion_diaria PUBLICADA para hoy, asignada al ejecutivo indicado
 */
public interface DemoAsignacionSeedApi {

    /**
     * Prepara el escenario de asignación demo. Es idempotente:
     * si ya existe una diaria PUBLICADA del ejecutivo para hoy, no hace nada.
     *
     * @param carteraId       UUID de la cartera a usar para la demo
     * @param ejecutivoId     UUID del ejecutivo terreno que recibirá la asignación
     * @param supervisorId    UUID del supervisor que publica
     * @param publicadorId    UUID del usuario que se registra como publicador (admin)
     * @param cantidadPersonas número de personas a incluir (2-5 recomendado)
     */
    void prepararEscenarioDemo(UUID carteraId, UUID ejecutivoId,
                               UUID supervisorId, UUID publicadorId,
                               int cantidadPersonas);
}
