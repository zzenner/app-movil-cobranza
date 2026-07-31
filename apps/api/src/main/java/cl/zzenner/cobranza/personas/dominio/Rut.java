package cl.zzenner.cobranza.personas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Rut {

    @Column(name = "rut_numero", nullable = false, length = 8)
    private String numero;

    @Column(name = "rut_dv", nullable = false, length = 1)
    private String dv;

    protected Rut() {}

    private Rut(String numero, String dv) {
        this.numero = numero;
        this.dv = dv;
    }

    public static Rut of(String numero, String dv) {
        if (numero == null || numero.isBlank()) {
            throw new RutInvalidoException("El número de RUT no puede ser nulo o vacío");
        }
        if (dv == null || dv.isBlank()) {
            throw new RutInvalidoException("El dígito verificador no puede ser nulo o vacío");
        }
        String numLimpio = numero.trim();
        String dvNorm = dv.trim().toUpperCase();

        if (!numLimpio.matches("^[0-9]+$")) {
            throw new RutInvalidoException("El número de RUT solo puede contener dígitos: " + numLimpio);
        }
        if (numLimpio.length() > 8) {
            throw new RutInvalidoException("El número de RUT no puede tener más de 8 dígitos: " + numLimpio);
        }
        if (!dvNorm.matches("^[0-9K]$")) {
            throw new RutInvalidoException("Dígito verificador inválido: " + dvNorm);
        }

        String dvCalculado = calcularDv(numLimpio);
        if (!dvCalculado.equals(dvNorm)) {
            throw new RutInvalidoException(
                    "Dígito verificador incorrecto para RUT " + numLimpio + ": esperado " + dvCalculado + ", recibido " + dvNorm);
        }

        return new Rut(numLimpio, dvNorm);
    }

    private static String calcularDv(String numero) {
        int suma = 0;
        int multiplicador = 2;
        for (int i = numero.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(numero.charAt(i)) * multiplicador;
            multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) return "0";
        if (resto == 10) return "K";
        return String.valueOf(resto);
    }

    public String getNumero() { return numero; }
    public String getDv() { return dv; }

    public String formato() { return numero + "-" + dv; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rut rut)) return false;
        return numero.equals(rut.numero) && dv.equals(rut.dv);
    }

    @Override
    public int hashCode() {
        return 31 * numero.hashCode() + dv.hashCode();
    }

    @Override
    public String toString() {
        return formato();
    }
}
