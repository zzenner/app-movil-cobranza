package cl.zzenner.cobranza.gestiones.api;

import java.util.List;
import java.util.UUID;

public interface GestionConsultaApi {

    List<DatosGestion> findUltimasGestiones(UUID personaId, int limite);
}
