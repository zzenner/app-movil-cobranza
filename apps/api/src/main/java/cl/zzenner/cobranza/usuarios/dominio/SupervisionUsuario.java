package cl.zzenner.cobranza.usuarios.dominio;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "supervision_usuarios")
public class SupervisionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "supervisor_id", nullable = false, updatable = false)
    private UUID supervisorId;

    @Column(name = "ejecutivo_id", nullable = false, updatable = false)
    private UUID ejecutivoId;

    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_termino")
    private LocalDate fechaTermino;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    protected SupervisionUsuario() {}

    public SupervisionUsuario(UUID supervisorId, UUID ejecutivoId, LocalDate fechaInicio) {
        if (supervisorId.equals(ejecutivoId)) {
            throw new RelacionSupervisionInvalidaException(
                    "El supervisor y el ejecutivo no pueden ser el mismo usuario.");
        }
        this.supervisorId = supervisorId;
        this.ejecutivoId = ejecutivoId;
        this.fechaInicio = fechaInicio;
        this.activo = true;
        this.fechaCreacion = Instant.now();
    }

    public void finalizar(LocalDate fechaTermino) {
        if (!activo) {
            throw new RelacionSupervisionInvalidaException("La relación de supervisión ya está finalizada.");
        }
        if (fechaTermino.isBefore(this.fechaInicio)) {
            throw new RelacionSupervisionInvalidaException(
                    "La fecha de término no puede ser anterior a la fecha de inicio.");
        }
        this.fechaTermino = fechaTermino;
        this.activo = false;
    }

    public UUID getId() { return id; }
    public UUID getSupervisorId() { return supervisorId; }
    public UUID getEjecutivoId() { return ejecutivoId; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaTermino() { return fechaTermino; }
    public boolean isActivo() { return activo; }
    public Instant getFechaCreacion() { return fechaCreacion; }
}
