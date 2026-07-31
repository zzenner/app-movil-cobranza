package cl.zzenner.cobranza.personas.infraestructura;

import cl.zzenner.cobranza.personas.dominio.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DireccionRepository extends JpaRepository<Direccion, UUID> {

    List<Direccion> findByPersonaId(UUID personaId);

    List<Direccion> findByPersonaIdAndVigenteTrue(UUID personaId);

    @Modifying
    @Query("UPDATE Direccion d SET d.esPrincipal = false WHERE d.personaId = :personaId AND d.esPrincipal = true")
    void desmarcarPrincipalDePersona(UUID personaId);
}
