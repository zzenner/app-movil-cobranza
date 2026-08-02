package cl.zzenner.cobranza.operaciones.api;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OperacionConsultaApi {

    /**
     * Retorna operaciones ACTIVA con sus cuotas relevantes (VENCIDA, VIGENTE, FUTURA)
     * para el conjunto de personas indicado. Una sola llamada evita N+1.
     */
    List<DatosOperacion> findOperacionesActivasConCuotas(Collection<UUID> personaIds);
}
