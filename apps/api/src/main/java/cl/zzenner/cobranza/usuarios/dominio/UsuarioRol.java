package cl.zzenner.cobranza.usuarios.dominio;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "usuario_roles")
public class UsuarioRol {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "rol_id", nullable = false, updatable = false)
    private UUID rolId;

    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private Instant fechaAsignacion;

    @Column(name = "fecha_termino")
    private Instant fechaTermino;

    @Column(name = "asignado_por")
    private UUID asignadoPor;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    protected UsuarioRol() {}

    public UsuarioRol(UUID usuarioId, UUID rolId, UUID asignadoPor) {
        this.usuarioId = usuarioId;
        this.rolId = rolId;
        this.asignadoPor = asignadoPor;
        this.fechaAsignacion = Instant.now();
        this.activo = true;
    }

    public void finalizar() {
        this.activo = false;
        this.fechaTermino = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public UUID getRolId() { return rolId; }
    public Instant getFechaAsignacion() { return fechaAsignacion; }
    public Instant getFechaTermino() { return fechaTermino; }
    public UUID getAsignadoPor() { return asignadoPor; }
    public boolean isActivo() { return activo; }
}
