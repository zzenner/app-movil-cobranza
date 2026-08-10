package cl.zzenner.cobranza.importacion.dominio;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(schema = "cobranza", name = "errores_importacion")
public class ErrorImportacion {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "importacion_id", nullable = false, updatable = false)
    private UUID importacionId;

    @Column(name = "numero_fila")
    private Integer numeroFila;

    @Column(name = "columna", length = 100)
    private String columna;

    @Column(name = "codigo_error", nullable = false, length = 100)
    private String codigoError;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false, length = 20)
    private NivelError nivel;

    @Column(name = "mensaje", nullable = false)
    private String mensaje;

    protected ErrorImportacion() {}

    public ErrorImportacion(UUID importacionId, Integer numeroFila, String columna,
                             String codigoError, NivelError nivel, String mensaje) {
        this.id = UUID.randomUUID();
        this.importacionId = importacionId;
        this.numeroFila = numeroFila;
        this.columna = columna;
        this.codigoError = codigoError;
        this.nivel = nivel;
        this.mensaje = mensaje;
    }

    public UUID getId() { return id; }
    public UUID getImportacionId() { return importacionId; }
    public Integer getNumeroFila() { return numeroFila; }
    public String getColumna() { return columna; }
    public String getCodigoError() { return codigoError; }
    public NivelError getNivel() { return nivel; }
    public String getMensaje() { return mensaje; }
}
