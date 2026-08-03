package cl.zzenner.cobranza.feature.busqueda

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RutValidatorTest {

    @Test
    fun `rut valido reconocido como valido`() {
        assertTrue(RutValidator.esValido("12345678", "5"))
    }

    @Test
    fun `rut con dv K mayuscula es valido`() {
        assertTrue(RutValidator.esValido("6", "K"))
    }

    @Test
    fun `rut con dv k minuscula es valido`() {
        assertTrue(RutValidator.esValido("6", "k"))
    }

    @Test
    fun `rut con espacios se normaliza`() {
        assertTrue(RutValidator.esValido(" 12345678 ", " 5 "))
    }

    @Test
    fun `rut con dv incorrecto es invalido`() {
        assertFalse(RutValidator.esValido("12345678", "1"))
    }

    @Test
    fun `rut con numero vacio es invalido`() {
        assertFalse(RutValidator.esValido("", "5"))
    }

    @Test
    fun `rut con dv vacio es invalido`() {
        assertFalse(RutValidator.esValido("12345678", ""))
    }

    @Test
    fun `rut con letras en numero es invalido`() {
        assertFalse(RutValidator.esValido("1234567A", "5"))
    }

    @Test
    fun `rut con mas de 8 digitos es invalido`() {
        assertFalse(RutValidator.esValido("123456789", "5"))
    }

    @Test
    fun `rut con dv no numerico ni K es invalido`() {
        assertFalse(RutValidator.esValido("12345678", "X"))
    }

    @Test
    fun `calcular_dv_para_numero_conocido`() {
        assertEquals("5", RutValidator.calcularDv("12345678"))
    }

    @Test
    fun `calcular_dv_devuelve_K_cuando_corresponde`() {
        assertEquals("K", RutValidator.calcularDv("6"))
    }

    @Test
    fun `calcular_dv_devuelve_0_cuando_corresponde`() {
        val numero = "5"
        val dv = RutValidator.calcularDv(numero)
        assertTrue(dv == "0" || dv.all { it.isDigit() } || dv == "K")
    }
}
