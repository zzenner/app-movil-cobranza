package cl.zzenner.cobranza.importacion.dominio;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "importaciones_mensuales")
public class ImportacionMensual {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "cartera_id", nullable = false, updatable = false)
    private UUID carteraId;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "periodo", nullable = false, updatable = false, length = 7)
    private String periodo;

    @Column(name = "sistema_origen", nullable = false, updatable = false, length = 50)
    private String sistemaOrigen;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoImportacion estado;

    @Column(name = "hash_archivo", nullable = false, updatable = false, length = 64)
    private String hashArchivo;

    @Column(name = "nombre_archivo_original", nullable = false, updatable = false, length = 500)
    private String nombreArchivoOriginal;

    @Column(name = "ruta_archivo")
    private String rutaArchivo;

    @Column(name = "filas_totales")
    private Integer filasTotales;

    @Column(name = "filas_procesadas")
    private Integer filasProcesadas;

    @Column(name = "filas_rechazadas")
    private Integer filasRechazadas;

    @Column(name = "filas_advertencia")
    private Integer filasAdvertencia;

    @Column(name = "personas_creadas")
    private Integer personasCreadas;

    @Column(name = "personas_actualizadas")
    private Integer personasActualizadas;

    @Column(name = "operaciones_creadas")
    private Integer operacionesCreadas;

    @Column(name = "operaciones_actualizadas")
    private Integer operacionesActualizadas;

    @Column(name = "cuotas_creadas")
    private Integer cuotasCreadas;

    @Column(name = "cuotas_actualizadas")
    private Integer cuotasActualizadas;

    @Column(name = "mensaje_error")
    private String mensajeError;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0;

    protected ImportacionMensual() {}

    public ImportacionMensual(UUID id, UUID carteraId, UUID usuarioId, String periodo,
                               String sistemaOrigen, String hashArchivo,
                               String nombreArchivoOriginal, String rutaArchivo) {
        this.id = id;
        this.carteraId = carteraId;
        this.usuarioId = usuarioId;
        this.periodo = periodo;
        this.sistemaOrigen = sistemaOrigen;
        this.hashArchivo = hashArchivo;
        this.nombreArchivoOriginal = nombreArchivoOriginal;
        this.rutaArchivo = rutaArchivo;
        this.estado = EstadoImportacion.RECIBIDA;
        Instant ahora = Instant.now();
        this.fechaCreacion = ahora;
        this.fechaActualizacion = ahora;
    }

    public void transicionarA(EstadoImportacion nuevoEstado) {
        this.estado = nuevoEstado;
        this.fechaActualizacion = Instant.now();
    }

    public void registrarResultadoValidacion(int totales, int rechazadas, int advertencias) {
        this.filasTotales = totales;
        this.filasRechazadas = rechazadas;
        this.filasAdvertencia = advertencias;
        this.fechaActualizacion = Instant.now();
    }

    public void registrarResultadoProcesamiento(int procesadas, int rechazadas, int advertencias,
                                                 int personasCreadas, int personasActualizadas,
                                                 int operacionesCreadas, int operacionesActualizadas,
                                                 int cuotasCreadas, int cuotasActualizadas) {
        this.filasProcesadas = procesadas;
        this.filasRechazadas = rechazadas;
        this.filasAdvertencia = advertencias;
        this.personasCreadas = personasCreadas;
        this.personasActualizadas = personasActualizadas;
        this.operacionesCreadas = operacionesCreadas;
        this.operacionesActualizadas = operacionesActualizadas;
        this.cuotasCreadas = cuotasCreadas;
        this.cuotasActualizadas = cuotasActualizadas;
        this.fechaActualizacion = Instant.now();
    }

    public void registrarError(String mensaje) {
        this.mensajeError = mensaje;
        this.fechaActualizacion = Instant.now();
    }

    public void limpiarRutaArchivo() {
        this.rutaArchivo = null;
        this.fechaActualizacion = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCarteraId() { return carteraId; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getPeriodo() { return periodo; }
    public String getSistemaOrigen() { return sistemaOrigen; }
    public EstadoImportacion getEstado() { return estado; }
    public String getHashArchivo() { return hashArchivo; }
    public String getNombreArchivoOriginal() { return nombreArchivoOriginal; }
    public String getRutaArchivo() { return rutaArchivo; }
    public Integer getFilasTotales() { return filasTotales; }
    public Integer getFilasProcesadas() { return filasProcesadas; }
    public Integer getFilasRechazadas() { return filasRechazadas; }
    public Integer getFilasAdvertencia() { return filasAdvertencia; }
    public Integer getPersonasCreadas() { return personasCreadas; }
    public Integer getPersonasActualizadas() { return personasActualizadas; }
    public Integer getOperacionesCreadas() { return operacionesCreadas; }
    public Integer getOperacionesActualizadas() { return operacionesActualizadas; }
    public Integer getCuotasCreadas() { return cuotasCreadas; }
    public Integer getCuotasActualizadas() { return cuotasActualizadas; }
    public String getMensajeError() { return mensajeError; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public long getVersion() { return version; }
}
