package cl.zzenner.cobranza.usuarios.api;

import com.fasterxml.jackson.annotation.JsonIgnoreType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Datos del usuario necesarios para el proceso de autenticación.
 * Transporta el hash de contraseña a través del límite de módulo de forma controlada.
 *
 * Restricciones de seguridad:
 * - @JsonIgnoreType impide serialización JSON accidental.
 * - toString() excluye el hash.
 * - Solo debe usarse en código de autenticación (AutenticacionService, UserDetailsService).
 * - Nunca incluir en respuestas HTTP ni en logs.
 */
@JsonIgnoreType
public final class CredencialesUsuario {

    private final UUID id;
    private final String nombreUsuario;
    private final String contrasenaHash;
    private final boolean activo;
    private final boolean bloqueado;
    private final Instant bloqueadoHasta;
    private final int intentosFallidos;
    private final List<String> codigosRoles;
    private final List<String> codigosPermisos;

    public CredencialesUsuario(UUID id, String nombreUsuario, String contrasenaHash,
                                boolean activo, boolean bloqueado, Instant bloqueadoHasta,
                                int intentosFallidos, List<String> codigosRoles,
                                List<String> codigosPermisos) {
        this.id = id;
        this.nombreUsuario = nombreUsuario;
        this.contrasenaHash = contrasenaHash;
        this.activo = activo;
        this.bloqueado = bloqueado;
        this.bloqueadoHasta = bloqueadoHasta;
        this.intentosFallidos = intentosFallidos;
        this.codigosRoles = List.copyOf(codigosRoles);
        this.codigosPermisos = List.copyOf(codigosPermisos);
    }

    public UUID getId() { return id; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getContrasenaHash() { return contrasenaHash; }
    public boolean isActivo() { return activo; }
    public boolean isBloqueado() { return bloqueado; }
    public Instant getBloqueadoHasta() { return bloqueadoHasta; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public List<String> getCodigosRoles() { return codigosRoles; }
    public List<String> getCodigosPermisos() { return codigosPermisos; }

    @Override
    public String toString() {
        return "CredencialesUsuario[id=" + id + ", nombreUsuario='" + nombreUsuario + "']";
    }
}
