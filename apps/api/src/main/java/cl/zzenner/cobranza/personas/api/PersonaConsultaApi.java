package cl.zzenner.cobranza.personas.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonaConsultaApi {

    Optional<DatosPersona> findById(UUID id);

    boolean existe(UUID id);

    Optional<DatosPersona> findByRut(String rutNumero, String rutDv);

    boolean personaActivaEnCartera(UUID personaId, UUID carteraId);

    List<DatosPersona> findAllByIds(Collection<UUID> ids);

    List<DatosDireccion> findDireccionesVigentesPorPersonas(Collection<UUID> personaIds);

    List<DatosAval> findAvalesPorPersonas(Collection<UUID> personaIds);

    List<UUID> findIdsByCarteraIdActiva(UUID carteraId, int limit);
}
