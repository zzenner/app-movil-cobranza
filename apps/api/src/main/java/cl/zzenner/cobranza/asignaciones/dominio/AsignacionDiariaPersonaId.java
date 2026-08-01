package cl.zzenner.cobranza.asignaciones.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class AsignacionDiariaPersonaId implements Serializable {

    @Column(name = "asignacion_diaria_id", updatable = false, nullable = false)
    private UUID asignacionDiariaId;

    @Column(name = "persona_id", updatable = false, nullable = false)
    private UUID personaId;

    protected AsignacionDiariaPersonaId() {}

    public AsignacionDiariaPersonaId(UUID asignacionDiariaId, UUID personaId) {
        this.asignacionDiariaId = asignacionDiariaId;
        this.personaId = personaId;
    }

    public UUID getAsignacionDiariaId() { return asignacionDiariaId; }
    public UUID getPersonaId() { return personaId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AsignacionDiariaPersonaId that)) return false;
        return Objects.equals(asignacionDiariaId, that.asignacionDiariaId)
                && Objects.equals(personaId, that.personaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(asignacionDiariaId, personaId);
    }
}
