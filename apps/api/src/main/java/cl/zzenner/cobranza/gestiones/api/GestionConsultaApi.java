package cl.zzenner.cobranza.gestiones.api;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface GestionConsultaApi {

    List<DatosGestion> findUltimasGestiones(UUID personaId, int limite);

    List<DatosGestion> findUltimasGestionesPorPersonas(Collection<UUID> personaIds, int limitePorPersona);
}
