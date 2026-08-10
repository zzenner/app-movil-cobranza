package cl.zzenner.cobranza.importacion.dominio;

public class PeriodoAnteriorNoPermitidoException extends RuntimeException {
    private final String periodoSolicitado;
    private final String periodoMaximoPermitido;

    public PeriodoAnteriorNoPermitidoException(String periodoSolicitado, String periodoMaximoPermitido) {
        super("No se puede importar el periodo " + periodoSolicitado
                + ": existe una importación completada más reciente (" + periodoMaximoPermitido + ")");
        this.periodoSolicitado = periodoSolicitado;
        this.periodoMaximoPermitido = periodoMaximoPermitido;
    }

    public String getPeriodoSolicitado() { return periodoSolicitado; }
    public String getPeriodoMaximoPermitido() { return periodoMaximoPermitido; }
}
