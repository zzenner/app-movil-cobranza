package cl.zzenner.cobranza.gestiones.dominio;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "gestiones")
public class Gestion {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_gestion", nullable = false, updatable = false, length = 30)
    private OrigenGestion origenGestion;

    @Column(name = "asignacion_diaria_id", updatable = false)
    private UUID asignacionDiariaId;

    @Column(name = "persona_id", nullable = false, updatable = false)
    private UUID personaId;

    @Column(name = "ejecutivo_id", nullable = false, updatable = false)
    private UUID ejecutivoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_gestion", nullable = false, updatable = false, length = 30)
    private TipoGestion tipoGestion;

    @Column(name = "fecha_gestion", nullable = false, updatable = false)
    private Instant fechaGestion;

    @Column(name = "observacion", updatable = false)
    private String observacion;

    @Column(name = "observacion_direccion", updatable = false)
    private String observacionDireccion;

    @Column(name = "latitud", nullable = false, updatable = false)
    private double latitud;

    @Column(name = "longitud", nullable = false, updatable = false)
    private double longitud;

    @Column(name = "precision_metros", nullable = false, updatable = false)
    private float precisionMetros;

    @Column(name = "proveedor_gps", updatable = false, length = 50)
    private String proveedorGps;

    @Column(name = "ubicacion_simulada", nullable = false, updatable = false)
    private boolean ubicacionSimulada;

    @Column(name = "fecha_captura_gps", nullable = false, updatable = false)
    private Instant fechaCapturaGps;

    @Column(name = "fecha_compromiso", updatable = false)
    private LocalDate fechaCompromiso;

    @Column(name = "fecha_creacion_servidor", nullable = false, updatable = false)
    private Instant fechaCreacionServidor;

    protected Gestion() {}

    public Gestion(UUID id, OrigenGestion origenGestion, UUID asignacionDiariaId,
                   UUID personaId, UUID ejecutivoId, TipoGestion tipoGestion,
                   Instant fechaGestion, String observacion, String observacionDireccion,
                   double latitud, double longitud, float precisionMetros,
                   String proveedorGps, boolean ubicacionSimulada, Instant fechaCapturaGps,
                   LocalDate fechaCompromiso) {
        Objects.requireNonNull(id,              "id es obligatorio");
        Objects.requireNonNull(origenGestion,   "origenGestion es obligatorio");
        Objects.requireNonNull(personaId,       "personaId es obligatorio");
        Objects.requireNonNull(ejecutivoId,     "ejecutivoId es obligatorio");
        Objects.requireNonNull(tipoGestion,     "tipoGestion es obligatorio");
        Objects.requireNonNull(fechaGestion,    "fechaGestion es obligatoria");
        Objects.requireNonNull(fechaCapturaGps, "fechaCapturaGps es obligatoria");

        if (origenGestion == OrigenGestion.ASIGNACION_DIARIA && asignacionDiariaId == null) {
            throw new IllegalArgumentException(
                "ASIGNACION_DIARIA requiere asignacion_diaria_id");
        }
        if (origenGestion == OrigenGestion.BUSQUEDA_DIRECTA && asignacionDiariaId != null) {
            throw new IllegalArgumentException(
                "BUSQUEDA_DIRECTA no puede tener asignacion_diaria_id");
        }
        if (latitud < -90 || latitud > 90) {
            throw new IllegalArgumentException(
                "latitud fuera de rango [-90, 90]: " + latitud);
        }
        if (longitud < -180 || longitud > 180) {
            throw new IllegalArgumentException(
                "longitud fuera de rango [-180, 180]: " + longitud);
        }
        if (precisionMetros < 0) {
            throw new IllegalArgumentException(
                "precision_metros no puede ser negativa: " + precisionMetros);
        }
        if (tipoGestion == TipoGestion.COMPROMISO_PAGO && fechaCompromiso == null) {
            throw new IllegalArgumentException(
                "COMPROMISO_PAGO requiere fecha_compromiso");
        }
        if (tipoGestion != TipoGestion.COMPROMISO_PAGO && fechaCompromiso != null) {
            throw new IllegalArgumentException(
                "Solo COMPROMISO_PAGO puede tener fecha_compromiso; tipo actual: " + tipoGestion);
        }

        this.id = id;
        this.origenGestion = origenGestion;
        this.asignacionDiariaId = asignacionDiariaId;
        this.personaId = personaId;
        this.ejecutivoId = ejecutivoId;
        this.tipoGestion = tipoGestion;
        // Truncar a microsegundos: PostgreSQL TIMESTAMPTZ pierde nanosegundos al persistir.
        // Hacerlo aquí garantiza que el estado en memoria coincide con el estado en BD.
        this.fechaGestion = fechaGestion.truncatedTo(ChronoUnit.MICROS);
        this.observacion = observacion;
        this.observacionDireccion = observacionDireccion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.precisionMetros = precisionMetros;
        this.proveedorGps = proveedorGps;
        this.ubicacionSimulada = ubicacionSimulada;
        this.fechaCapturaGps = fechaCapturaGps.truncatedTo(ChronoUnit.MICROS);
        this.fechaCompromiso = fechaCompromiso;
        this.fechaCreacionServidor = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public boolean tieneContenidoConflictivo(Gestion otra) {
        return !Objects.equals(this.personaId, otra.personaId)
            || !Objects.equals(this.ejecutivoId, otra.ejecutivoId)
            || !Objects.equals(this.origenGestion, otra.origenGestion)
            || !Objects.equals(this.asignacionDiariaId, otra.asignacionDiariaId)
            || !Objects.equals(this.tipoGestion, otra.tipoGestion)
            || !Objects.equals(this.fechaGestion, otra.fechaGestion)
            || !Objects.equals(this.fechaCompromiso, otra.fechaCompromiso)
            || Double.compare(this.latitud, otra.latitud) != 0
            || Double.compare(this.longitud, otra.longitud) != 0;
    }

    public UUID getId()                   { return id; }
    public OrigenGestion getOrigenGestion() { return origenGestion; }
    public UUID getAsignacionDiariaId()  { return asignacionDiariaId; }
    public UUID getPersonaId()           { return personaId; }
    public UUID getEjecutivoId()         { return ejecutivoId; }
    public TipoGestion getTipoGestion()  { return tipoGestion; }
    public Instant getFechaGestion()     { return fechaGestion; }
    public String getObservacion()       { return observacion; }
    public String getObservacionDireccion() { return observacionDireccion; }
    public double getLatitud()           { return latitud; }
    public double getLongitud()          { return longitud; }
    public float getPrecisionMetros()    { return precisionMetros; }
    public String getProveedorGps()      { return proveedorGps; }
    public boolean isUbicacionSimulada() { return ubicacionSimulada; }
    public Instant getFechaCapturaGps()  { return fechaCapturaGps; }
    public LocalDate getFechaCompromiso() { return fechaCompromiso; }
    public Instant getFechaCreacionServidor() { return fechaCreacionServidor; }
}
