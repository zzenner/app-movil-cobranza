package cl.zzenner.cobranza.operaciones.dominio;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "operaciones")
public class Operacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "persona_id", nullable = false, updatable = false)
    private UUID personaId;

    @Column(name = "numero_operacion", nullable = false, length = 50)
    private String numeroOperacion;

    @Column(name = "identificador_externo", length = 100)
    private String identificadorExterno;

    @Column(name = "sistema_origen", nullable = false, length = 50)
    private String sistemaOrigen;

    @Column(name = "tipo_operacion", length = 50)
    private String tipoOperacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoOperacion estado;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda = "CLP";

    @Column(name = "capital", nullable = false, precision = 15, scale = 2)
    private BigDecimal capital;

    @Column(name = "interes_penal", nullable = false, precision = 15, scale = 2)
    private BigDecimal interesPenal = BigDecimal.ZERO;

    @Column(name = "gastos_cobranza", nullable = false, precision = 15, scale = 2)
    private BigDecimal gastosCobranza = BigDecimal.ZERO;

    @Column(name = "total_vigente", precision = 15, scale = 2)
    private BigDecimal totalVigente;

    @Column(name = "fecha_otorgamiento")
    private LocalDate fechaOtorgamiento;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

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

    protected Operacion() {}

    public Operacion(UUID personaId, String numeroOperacion, EstadoOperacion estado,
                     BigDecimal capital, String sistemaOrigen) {
        this.personaId = personaId;
        this.numeroOperacion = numeroOperacion;
        this.estado = estado;
        this.capital = capital;
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

    public void registrarImportacion(Instant fechaOrigen) {
        this.fechaActualizacionOrigen = fechaOrigen;
        this.fechaImportacion = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPersonaId() { return personaId; }
    public String getNumeroOperacion() { return numeroOperacion; }
    public String getIdentificadorExterno() { return identificadorExterno; }
    public String getSistemaOrigen() { return sistemaOrigen; }
    public String getTipoOperacion() { return tipoOperacion; }
    public EstadoOperacion getEstado() { return estado; }
    public String getMoneda() { return moneda; }
    public BigDecimal getCapital() { return capital; }
    public BigDecimal getInteresPenal() { return interesPenal; }
    public BigDecimal getGastosCobranza() { return gastosCobranza; }
    public BigDecimal getTotalVigente() { return totalVigente; }
    public LocalDate getFechaOtorgamiento() { return fechaOtorgamiento; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public Instant getFechaActualizacionOrigen() { return fechaActualizacionOrigen; }
    public Instant getFechaImportacion() { return fechaImportacion; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public long getVersion() { return version; }

    public void setEstado(EstadoOperacion estado) { this.estado = estado; }
    public void setCapital(BigDecimal capital) { this.capital = capital; }
    public void setInteresPenal(BigDecimal interesPenal) { this.interesPenal = interesPenal; }
    public void setGastosCobranza(BigDecimal gastosCobranza) { this.gastosCobranza = gastosCobranza; }
    public void setTotalVigente(BigDecimal totalVigente) { this.totalVigente = totalVigente; }
    public void setTipoOperacion(String tipoOperacion) { this.tipoOperacion = tipoOperacion; }
    public void setIdentificadorExterno(String identificadorExterno) { this.identificadorExterno = identificadorExterno; }
    public void setFechaOtorgamiento(LocalDate fechaOtorgamiento) { this.fechaOtorgamiento = fechaOtorgamiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
}
