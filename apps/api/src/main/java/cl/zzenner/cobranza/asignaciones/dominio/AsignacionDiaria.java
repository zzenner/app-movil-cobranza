package cl.zzenner.cobranza.asignaciones.dominio;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "asignaciones_diarias")
public class AsignacionDiaria {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "asignacion_mensual_id", nullable = false, updatable = false)
    private UUID asignacionMensualId;

    @Column(name = "ejecutivo_id", nullable = false, updatable = false)
    private UUID ejecutivoId;

    @Column(name = "supervisor_id", nullable = false, updatable = false)
    private UUID supervisorId;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoAsignacionDiaria estado;

    @Column(name = "fecha_publicacion")
    private Instant fechaPublicacion;

    @Column(name = "publicado_por_id")
    private UUID publicadoPorId;

    @Column(name = "motivo_cancelacion")
    private String motivoCancelacion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0;

    protected AsignacionDiaria() {}

    public AsignacionDiaria(UUID asignacionMensualId, UUID ejecutivoId, UUID supervisorId, LocalDate fecha) {
        this.id = UUID.randomUUID();
        this.asignacionMensualId = asignacionMensualId;
        this.ejecutivoId = ejecutivoId;
        this.supervisorId = supervisorId;
        this.fecha = fecha;
        this.estado = EstadoAsignacionDiaria.BORRADOR;
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

    public void publicar(UUID publicadoPorId) {
        if (this.estado != EstadoAsignacionDiaria.BORRADOR) {
            throw new TransicionEstadoInvalidaException(
                    "Solo se puede publicar desde BORRADOR. Estado actual: " + this.estado);
        }
        this.estado = EstadoAsignacionDiaria.PUBLICADA;
        this.fechaPublicacion = Instant.now();
        this.publicadoPorId = publicadoPorId;
    }

    public void finalizar() {
        if (this.estado != EstadoAsignacionDiaria.PUBLICADA) {
            throw new TransicionEstadoInvalidaException(
                    "Solo se puede finalizar desde PUBLICADA. Estado actual: " + this.estado);
        }
        this.estado = EstadoAsignacionDiaria.FINALIZADA;
    }

    public void cancelar(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de cancelación es obligatorio");
        }
        if (this.estado == EstadoAsignacionDiaria.FINALIZADA
                || this.estado == EstadoAsignacionDiaria.CANCELADA) {
            throw new TransicionEstadoInvalidaException(
                    "No se puede cancelar desde el estado: " + this.estado);
        }
        this.estado = EstadoAsignacionDiaria.CANCELADA;
        this.motivoCancelacion = motivo;
    }

    public UUID getId() { return id; }
    public UUID getAsignacionMensualId() { return asignacionMensualId; }
    public UUID getEjecutivoId() { return ejecutivoId; }
    public UUID getSupervisorId() { return supervisorId; }
    public LocalDate getFecha() { return fecha; }
    public EstadoAsignacionDiaria getEstado() { return estado; }
    public Instant getFechaPublicacion() { return fechaPublicacion; }
    public UUID getPublicadoPorId() { return publicadoPorId; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public long getVersion() { return version; }
}
