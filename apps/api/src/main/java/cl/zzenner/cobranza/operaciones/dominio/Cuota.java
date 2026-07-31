package cl.zzenner.cobranza.operaciones.dominio;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "cuotas")
public class Cuota {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "operacion_id", nullable = false, updatable = false)
    private UUID operacionId;

    @Column(name = "numero_cuota", nullable = false)
    private int numeroCuota;

    @Column(name = "identificador_externo", length = 100)
    private String identificadorExterno;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoCuota estado;

    @Column(name = "capital", precision = 15, scale = 2)
    private BigDecimal capital;

    @Column(name = "interes", precision = 15, scale = 2)
    private BigDecimal interes;

    @Column(name = "interes_penal", nullable = false, precision = 15, scale = 2)
    private BigDecimal interesPenal = BigDecimal.ZERO;

    @Column(name = "gastos_cobranza", nullable = false, precision = 15, scale = 2)
    private BigDecimal gastosCobranza = BigDecimal.ZERO;

    @Column(name = "monto_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "saldo", precision = 15, scale = 2)
    private BigDecimal saldo;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Column(name = "fecha_actualizacion_origen")
    private Instant fechaActualizacionOrigen;

    @Column(name = "fecha_importacion")
    private Instant fechaImportacion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    protected Cuota() {}

    public Cuota(UUID operacionId, int numeroCuota, EstadoCuota estado,
                 BigDecimal montoTotal, LocalDate fechaVencimiento) {
        this.operacionId = operacionId;
        this.numeroCuota = numeroCuota;
        this.estado = estado;
        this.montoTotal = montoTotal;
        this.fechaVencimiento = fechaVencimiento;
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
    public UUID getOperacionId() { return operacionId; }
    public int getNumeroCuota() { return numeroCuota; }
    public String getIdentificadorExterno() { return identificadorExterno; }
    public EstadoCuota getEstado() { return estado; }
    public BigDecimal getCapital() { return capital; }
    public BigDecimal getInteres() { return interes; }
    public BigDecimal getInteresPenal() { return interesPenal; }
    public BigDecimal getGastosCobranza() { return gastosCobranza; }
    public BigDecimal getMontoTotal() { return montoTotal; }
    public BigDecimal getSaldo() { return saldo; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public LocalDate getFechaPago() { return fechaPago; }
    public Instant getFechaActualizacionOrigen() { return fechaActualizacionOrigen; }
    public Instant getFechaImportacion() { return fechaImportacion; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }

    public void setEstado(EstadoCuota estado) { this.estado = estado; }
    public void setCapital(BigDecimal capital) { this.capital = capital; }
    public void setInteres(BigDecimal interes) { this.interes = interes; }
    public void setInteresPenal(BigDecimal interesPenal) { this.interesPenal = interesPenal; }
    public void setGastosCobranza(BigDecimal gastosCobranza) { this.gastosCobranza = gastosCobranza; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }
    public void setIdentificadorExterno(String identificadorExterno) { this.identificadorExterno = identificadorExterno; }
}
