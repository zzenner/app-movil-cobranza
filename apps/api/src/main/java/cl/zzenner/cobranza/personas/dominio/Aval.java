package cl.zzenner.cobranza.personas.dominio;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "avales")
public class Aval {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "persona_id", nullable = false, updatable = false)
    private UUID personaId;

    @Embedded
    private Rut rut;

    @Column(name = "nombre", nullable = false, length = 300)
    private String nombre;

    @Column(name = "codigo_externo", length = 100)
    private String codigoExterno;

    @Column(name = "sistema_origen", nullable = false, length = 50)
    private String sistemaOrigen;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    protected Aval() {}

    public Aval(UUID personaId, Rut rut, String nombre, String sistemaOrigen) {
        this.personaId = personaId;
        this.rut = rut;
        this.nombre = nombre;
        this.sistemaOrigen = sistemaOrigen;
    }

    @PrePersist
    void preCrear() {
        if (fechaCreacion == null) fechaCreacion = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPersonaId() { return personaId; }
    public Rut getRut() { return rut; }
    public String getNombre() { return nombre; }
    public String getCodigoExterno() { return codigoExterno; }
    public String getSistemaOrigen() { return sistemaOrigen; }
    public Instant getFechaCreacion() { return fechaCreacion; }

    public void setCodigoExterno(String codigoExterno) { this.codigoExterno = codigoExterno; }
}
