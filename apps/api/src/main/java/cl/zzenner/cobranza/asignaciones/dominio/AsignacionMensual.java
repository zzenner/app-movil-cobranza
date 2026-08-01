package cl.zzenner.cobranza.asignaciones.dominio;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "asignaciones_mensuales")
public class AsignacionMensual {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "cartera_id", nullable = false, updatable = false)
    private UUID carteraId;

    @Column(name = "ejecutivo_id", nullable = false, updatable = false)
    private UUID ejecutivoId;

    @Column(name = "supervisor_id", nullable = false, updatable = false)
    private UUID supervisorId;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "activa", nullable = false)
    private boolean activa;

    @Column(name = "observacion")
    private String observacion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0;

    protected AsignacionMensual() {}

    public AsignacionMensual(UUID carteraId, UUID ejecutivoId, UUID supervisorId,
                              LocalDate fechaInicio, LocalDate fechaFin, String observacion) {
        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("fecha_fin no puede ser anterior a fecha_inicio");
        }
        this.id = UUID.randomUUID();
        this.carteraId = carteraId;
        this.ejecutivoId = ejecutivoId;
        this.supervisorId = supervisorId;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activa = true;
        this.observacion = observacion;
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

    public void cerrar() {
        if (!this.activa) {
            throw new IllegalStateException("La asignación mensual ya está cerrada");
        }
        this.activa = false;
    }

    public UUID getId() { return id; }
    public UUID getCarteraId() { return carteraId; }
    public UUID getEjecutivoId() { return ejecutivoId; }
    public UUID getSupervisorId() { return supervisorId; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public boolean isActiva() { return activa; }
    public String getObservacion() { return observacion; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public long getVersion() { return version; }
}
