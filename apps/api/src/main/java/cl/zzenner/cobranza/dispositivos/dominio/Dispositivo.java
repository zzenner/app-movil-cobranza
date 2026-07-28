package cl.zzenner.cobranza.dispositivos.dominio;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "dispositivos")
public class Dispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "identificador_instalacion", nullable = false, updatable = false, length = 36)
    private String identificadorInstalacion;

    @Column(name = "nombre_dispositivo", length = 200)
    private String nombreDispositivo;

    @Column(name = "fabricante", length = 100)
    private String fabricante;

    @Column(name = "modelo", length = 100)
    private String modelo;

    @Column(name = "version_android", length = 20)
    private String versionAndroid;

    @Column(name = "version_aplicacion", length = 20)
    private String versionAplicacion;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "revocado", nullable = false)
    private boolean revocado = false;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    @Column(name = "fecha_ultima_sincronizacion")
    private Instant fechaUltimaSincronizacion;

    @Column(name = "fecha_revocacion")
    private Instant fechaRevocacion;

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0;

    protected Dispositivo() {}

    public Dispositivo(UUID usuarioId, String identificadorInstalacion, String nombreDispositivo,
                       String fabricante, String modelo, String versionAndroid, String versionAplicacion) {
        this.usuarioId = usuarioId;
        this.identificadorInstalacion = identificadorInstalacion;
        this.nombreDispositivo = nombreDispositivo;
        this.fabricante = fabricante;
        this.modelo = modelo;
        this.versionAndroid = versionAndroid;
        this.versionAplicacion = versionAplicacion;
        this.fechaRegistro = Instant.now();
        this.activo = true;
        this.revocado = false;
    }

    /**
     * Revoca el dispositivo. Un dispositivo revocado queda inactivo
     * y registra la fecha de revocación. No puede reactivarse sin
     * operación administrativa explícita.
     */
    public void revocar() {
        if (revocado) {
            throw new DispositivoRevocadoException(id);
        }
        this.revocado = true;
        this.activo = false;
        this.fechaRevocacion = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getIdentificadorInstalacion() { return identificadorInstalacion; }
    public String getNombreDispositivo() { return nombreDispositivo; }
    public String getFabricante() { return fabricante; }
    public String getModelo() { return modelo; }
    public String getVersionAndroid() { return versionAndroid; }
    public String getVersionAplicacion() { return versionAplicacion; }
    public boolean isActivo() { return activo; }
    public boolean isRevocado() { return revocado; }
    public Instant getFechaRegistro() { return fechaRegistro; }
    public Instant getFechaUltimaSincronizacion() { return fechaUltimaSincronizacion; }
    public Instant getFechaRevocacion() { return fechaRevocacion; }
    public long getVersion() { return version; }

    public void setVersionAplicacion(String versionAplicacion) {
        this.versionAplicacion = versionAplicacion;
    }

    public void registrarSincronizacion(String versionAplicacion) {
        this.fechaUltimaSincronizacion = Instant.now();
        this.versionAplicacion = versionAplicacion;
    }

    @Override
    public String toString() {
        return "Dispositivo{id=" + id + ", usuarioId=" + usuarioId
                + ", identificadorInstalacion='" + identificadorInstalacion
                + "', activo=" + activo + ", revocado=" + revocado + "}";
    }
}
