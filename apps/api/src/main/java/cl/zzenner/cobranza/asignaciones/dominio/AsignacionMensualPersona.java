package cl.zzenner.cobranza.asignaciones.dominio;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "asignaciones_mensuales_personas")
public class AsignacionMensualPersona {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "asignacion_mensual_id", nullable = false, updatable = false)
    private UUID asignacionMensualId;

    @Column(name = "persona_id", nullable = false, updatable = false)
    private UUID personaId;

    @Column(name = "cartera_id", nullable = false, updatable = false)
    private UUID carteraId;

    @Column(name = "activa", nullable = false)
    private boolean activa;

    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0;

    protected AsignacionMensualPersona() {}

    public AsignacionMensualPersona(UUID asignacionMensualId, UUID personaId, UUID carteraId, LocalDate fechaInicio) {
        this.id = UUID.randomUUID();
        this.asignacionMensualId = asignacionMensualId;
        this.personaId = personaId;
        this.carteraId = carteraId;
        this.activa = true;
        this.fechaInicio = fechaInicio;
    }

    @PrePersist
    void preCrear() {
        Instant ahora = Instant.now();
        if (fechaCreacion == null) fechaCreacion = ahora;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void preActualizar() {
        fechaActualizacion = Instant.now();
    }

    public void cerrar(LocalDate fechaFin) {
        if (!this.activa) {
            throw new IllegalStateException("El vínculo ya está cerrado");
        }
        if (fechaFin.isBefore(this.fechaInicio)) {
            throw new IllegalArgumentException("fecha_fin no puede ser anterior a fecha_inicio");
        }
        this.fechaFin = fechaFin;
        this.activa = false;
    }

    public UUID getId() { return id; }
    public UUID getAsignacionMensualId() { return asignacionMensualId; }
    public UUID getPersonaId() { return personaId; }
    public UUID getCarteraId() { return carteraId; }
    public boolean isActiva() { return activa; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public long getVersion() { return version; }
}
