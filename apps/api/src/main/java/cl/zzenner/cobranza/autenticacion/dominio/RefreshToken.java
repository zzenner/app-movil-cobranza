package cl.zzenner.cobranza.autenticacion.dominio;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "refresh_tokens")
public class RefreshToken {

    public enum Estado { ACTIVO, CONSUMIDO, REVOCADO }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "sesion_id", nullable = false, updatable = false)
    private UUID sesionId;

    @Column(name = "hash_token", nullable = false, updatable = false, length = 64)
    private String hashToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_vencimiento", nullable = false)
    private Instant fechaVencimiento;

    @Column(name = "fecha_consumo")
    private Instant fechaConsumo;

    @Column(name = "fecha_revocacion")
    private Instant fechaRevocacion;

    protected RefreshToken() {}

    public RefreshToken(UUID sesionId, String hashToken, Instant fechaVencimiento) {
        this.sesionId = sesionId;
        this.hashToken = hashToken;
        this.fechaVencimiento = fechaVencimiento;
        this.fechaCreacion = Instant.now();
        this.estado = Estado.ACTIVO;
    }

    public void consumir(Instant momento) {
        this.estado = Estado.CONSUMIDO;
        this.fechaConsumo = momento;
    }

    public void revocar(Instant momento) {
        this.estado = Estado.REVOCADO;
        this.fechaRevocacion = momento;
    }

    public boolean estaActivo() { return estado == Estado.ACTIVO; }
    public boolean estaVigente(Instant ahora) { return estaActivo() && ahora.isBefore(fechaVencimiento); }

    public UUID getId() { return id; }
    public UUID getSesionId() { return sesionId; }
    public String getHashToken() { return hashToken; }
    public Estado getEstado() { return estado; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaVencimiento() { return fechaVencimiento; }
    public Instant getFechaConsumo() { return fechaConsumo; }
    public Instant getFechaRevocacion() { return fechaRevocacion; }
}
