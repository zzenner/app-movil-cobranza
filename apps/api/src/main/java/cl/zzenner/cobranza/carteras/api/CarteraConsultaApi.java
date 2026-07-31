package cl.zzenner.cobranza.carteras.api;

import java.util.Optional;
import java.util.UUID;

public interface CarteraConsultaApi {

    Optional<DatosCartera> findById(UUID id);

    boolean existeActiva(UUID id);
}
