package cl.zzenner.cobranza.importacion.dominio;

public enum EstadoImportacion {
    RECIBIDA,
    VALIDANDO,
    VALIDADA,
    CON_ERRORES,
    PROCESANDO,
    COMPLETADA,
    FALLIDA,
    EXPIRADA;

    public boolean esTerminal() {
        return this == COMPLETADA || this == FALLIDA || this == EXPIRADA;
    }

    public boolean permiteConfirmar() {
        return this == VALIDADA;
    }

    public boolean archivoPuedeExistir() {
        return this == VALIDADA || this == VALIDANDO || this == PROCESANDO;
    }
}
