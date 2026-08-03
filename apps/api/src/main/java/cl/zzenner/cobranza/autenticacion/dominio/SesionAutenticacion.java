package cl.zzenner.cobranza.autenticacion.dominio;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "sesiones_autenticacion")
public class SesionAutenticacion {

    public enum Estado { ACTIVA, CERRADA, COMPROMETIDA }

    public enum MotivoCierre { LOGOUT, EXPIRADA, REVOCACION_DISPOSITIVO, COMPROMETIDA }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "dispositivo_id", updatable = false)
    private UUID dispositivoId;

    @Column(name = "tipo_cliente", nullable = false, updatable = false, length = 10)
    private String tipoCliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.ACTIVA;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_ultimo_acceso", nullable = false)
    private Instant fechaUltimoAcceso;

    @Column(name = "fecha_vencimiento_abs", nullable = false)
    private Instant fechaVencimientoAbs;

    @Column(name = "fecha_cierre")
    private Instant fechaCierre;

    @Column(name = "motivo_cierre", length = 30)
    private String motivoCierre;

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0;

    protected SesionAutenticacion() {}

    public SesionAutenticacion(UUID usuarioId, UUID dispositivoId, String ipOrigen,
                                String userAgent, Instant fechaVencimientoAbs) {
        this.usuarioId = usuarioId;
        this.dispositivoId = dispositivoId;
        this.tipoCliente = "ANDROID";
        this.ipOrigen = ipOrigen;
        this.userAgent = userAgent;
        this.fechaVencimientoAbs = fechaVencimientoAbs;
        Instant ahora = Instant.now();
        this.fechaCreacion = ahora;
        this.fechaUltimoAcceso = ahora;
        this.estado = Estado.ACTIVA;
    }

    public SesionAutenticacion(UUID usuarioId, String ipOrigen,
                                String userAgent, Instant fechaVencimientoAbs) {
        this.usuarioId = usuarioId;
        this.dispositivoId = null;
        this.tipoCliente = "WEB";
        this.ipOrigen = ipOrigen;
        this.userAgent = userAgent;
        this.fechaVencimientoAbs = fechaVencimientoAbs;
        Instant ahora = Instant.now();
        this.fechaCreacion = ahora;
        this.fechaUltimoAcceso = ahora;
        this.estado = Estado.ACTIVA;
    }

    public void cerrar(MotivoCierre motivo) {
        if (this.estado != Estado.ACTIVA) return;
        this.estado = Estado.CERRADA;
        this.fechaCierre = Instant.now();
        this.motivoCierre = motivo.name();
    }

    public void comprometer() {
        this.estado = Estado.COMPROMETIDA;
        this.fechaCierre = Instant.now();
        this.motivoCierre = MotivoCierre.COMPROMETIDA.name();
    }

    public void actualizarUltimoAcceso(Instant momento) {
        this.fechaUltimoAcceso = momento;
    }

    public boolean estaActiva() { return estado == Estado.ACTIVA; }
    public boolean estaVigente(Instant ahora) { return estaActiva() && ahora.isBefore(fechaVencimientoAbs); }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public UUID getDispositivoId() { return dispositivoId; }
    public String getTipoCliente() { return tipoCliente; }
    public boolean esWeb() { return "WEB".equals(tipoCliente); }
    public Estado getEstado() { return estado; }
    public String getIpOrigen() { return ipOrigen; }
    public String getUserAgent() { return userAgent; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaUltimoAcceso() { return fechaUltimoAcceso; }
    public Instant getFechaVencimientoAbs() { return fechaVencimientoAbs; }
    public Instant getFechaCierre() { return fechaCierre; }
    public String getMotivoCierre() { return motivoCierre; }
    public long getVersion() { return version; }
}
