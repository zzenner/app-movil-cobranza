package cl.zzenner.cobranza.usuarios.dominio;

import jakarta.persistence.*;

@Entity
@Table(schema = "cobranza", name = "rol_permisos")
public class RolPermiso {

    @EmbeddedId
    private RolPermisoId id;

    protected RolPermiso() {}

    public RolPermiso(RolPermisoId id) {
        this.id = id;
    }

    public RolPermisoId getId() { return id; }

    @Override
    public String toString() {
        return "RolPermiso{rolId=" + id.getRolId() + ", permisoId=" + id.getPermisoId() + "}";
    }
}
