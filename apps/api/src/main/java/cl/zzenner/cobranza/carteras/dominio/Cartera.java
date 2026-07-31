package cl.zzenner.cobranza.carteras.dominio;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "carteras")
public class Cartera {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0;

    protected Cartera() {}

    public Cartera(String nombre, String descripcion) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la cartera no puede ser nulo o vacío");
        }
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activa = true;
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

    public void activar() {
        this.activa = true;
    }

    public void desactivar() {
        this.activa = false;
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public boolean isActiva() { return activa; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public long getVersion() { return version; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
