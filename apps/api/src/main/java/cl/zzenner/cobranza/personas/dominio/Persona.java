package cl.zzenner.cobranza.personas.dominio;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "personas")
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Embedded
    private Rut rut;

    @Column(name = "nombre", nullable = false, length = 300)
    private String nombre;

    @Column(name = "cartera_id")
    private UUID carteraId;

    @Column(name = "codigo_externo", length = 100)
    private String codigoExterno;

    @Column(name = "sistema_origen", nullable = false, length = 50)
    private String sistemaOrigen;

    @Column(name = "fecha_actualizacion_origen")
    private Instant fechaActualizacionOrigen;

    @Column(name = "fecha_importacion")
    private Instant fechaImportacion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0;

    protected Persona() {}

    public Persona(Rut rut, String nombre, String sistemaOrigen) {
        this.rut = rut;
        this.nombre = nombre;
        this.sistemaOrigen = sistemaOrigen;
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

    public void asignarCartera(UUID carteraId) {
        this.carteraId = carteraId;
    }

    public void quitarCartera() {
        this.carteraId = null;
    }

    public void registrarImportacion(Instant fechaOrigen) {
        this.fechaActualizacionOrigen = fechaOrigen;
        this.fechaImportacion = Instant.now();
    }

    public UUID getId() { return id; }
    public Rut getRut() { return rut; }
    public String getNombre() { return nombre; }
    public UUID getCarteraId() { return carteraId; }
    public String getCodigoExterno() { return codigoExterno; }
    public String getSistemaOrigen() { return sistemaOrigen; }
    public Instant getFechaActualizacionOrigen() { return fechaActualizacionOrigen; }
    public Instant getFechaImportacion() { return fechaImportacion; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public long getVersion() { return version; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setCodigoExterno(String codigoExterno) { this.codigoExterno = codigoExterno; }
}
