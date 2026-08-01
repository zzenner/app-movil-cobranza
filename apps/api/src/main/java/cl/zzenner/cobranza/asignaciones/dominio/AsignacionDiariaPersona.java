package cl.zzenner.cobranza.asignaciones.dominio;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "asignaciones_diarias_personas")
public class AsignacionDiariaPersona {

    @EmbeddedId
    private AsignacionDiariaPersonaId id;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    protected AsignacionDiariaPersona() {}

    public AsignacionDiariaPersona(UUID asignacionDiariaId, UUID personaId) {
        this.id = new AsignacionDiariaPersonaId(asignacionDiariaId, personaId);
        this.fechaCreacion = Instant.now();
    }

    public AsignacionDiariaPersonaId getId() { return id; }
    public Instant getFechaCreacion() { return fechaCreacion; }
}
