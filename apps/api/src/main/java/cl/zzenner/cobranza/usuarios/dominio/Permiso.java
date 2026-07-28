package cl.zzenner.cobranza.usuarios.dominio;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "permisos")
public class Permiso {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "codigo", nullable = false, length = 100)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    protected Permiso() {}

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public boolean isActivo() { return activo; }

    @Override
    public String toString() {
        return "Permiso{id=" + id + ", codigo='" + codigo + "'}";
    }
}
