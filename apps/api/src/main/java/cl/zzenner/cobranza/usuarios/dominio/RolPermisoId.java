package cl.zzenner.cobranza.usuarios.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class RolPermisoId implements Serializable {

    @Column(name = "rol_id", nullable = false)
    private UUID rolId;

    @Column(name = "permiso_id", nullable = false)
    private UUID permisoId;

    protected RolPermisoId() {}

    public RolPermisoId(UUID rolId, UUID permisoId) {
        this.rolId = rolId;
        this.permisoId = permisoId;
    }

    public UUID getRolId() { return rolId; }
    public UUID getPermisoId() { return permisoId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RolPermisoId that)) return false;
        return Objects.equals(rolId, that.rolId) && Objects.equals(permisoId, that.permisoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rolId, permisoId);
    }
}
