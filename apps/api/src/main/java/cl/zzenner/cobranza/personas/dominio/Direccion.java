package cl.zzenner.cobranza.personas.dominio;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "direcciones")
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "persona_id", nullable = false, updatable = false)
    private UUID personaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoDireccion tipo;

    @Column(name = "texto", nullable = false)
    private String texto;

    @Column(name = "comuna", length = 100)
    private String comuna;

    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "referencia")
    private String referencia;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal = false;

    @Column(name = "vigente", nullable = false)
    private boolean vigente = true;

    @Column(name = "codigo_externo", length = 100)
    private String codigoExterno;

    @Column(name = "sistema_origen", nullable = false, length = 50)
    private String sistemaOrigen;

    @Column(name = "fecha_actualizacion_origen")
    private Instant fechaActualizacionOrigen;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    protected Direccion() {}

    public Direccion(UUID personaId, TipoDireccion tipo, String texto, String sistemaOrigen) {
        this.personaId = personaId;
        this.tipo = tipo;
        this.texto = texto;
        this.sistemaOrigen = sistemaOrigen;
    }

    @PrePersist
    void preCrear() {
        if (fechaCreacion == null) fechaCreacion = Instant.now();
    }

    public void marcarPrincipal() {
        this.esPrincipal = true;
    }

    public void desmarcarPrincipal() {
        this.esPrincipal = false;
    }

    public void desactivar() {
        this.vigente = false;
        this.esPrincipal = false;
    }

    public UUID getId() { return id; }
    public UUID getPersonaId() { return personaId; }
    public TipoDireccion getTipo() { return tipo; }
    public String getTexto() { return texto; }
    public String getComuna() { return comuna; }
    public String getCiudad() { return ciudad; }
    public String getRegion() { return region; }
    public String getReferencia() { return referencia; }
    public boolean isEsPrincipal() { return esPrincipal; }
    public boolean isVigente() { return vigente; }
    public String getCodigoExterno() { return codigoExterno; }
    public String getSistemaOrigen() { return sistemaOrigen; }
    public Instant getFechaActualizacionOrigen() { return fechaActualizacionOrigen; }
    public Instant getFechaCreacion() { return fechaCreacion; }

    public void setComuna(String comuna) { this.comuna = comuna; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public void setRegion(String region) { this.region = region; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public void setCodigoExterno(String codigoExterno) { this.codigoExterno = codigoExterno; }
    public void setFechaActualizacionOrigen(Instant fechaActualizacionOrigen) {
        this.fechaActualizacionOrigen = fechaActualizacionOrigen;
    }
}
