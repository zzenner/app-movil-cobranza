package cl.zzenner.cobranza.feature.gestion

import cl.zzenner.cobranza.feature.gestion.domain.ErrorValidacion
import cl.zzenner.cobranza.feature.gestion.domain.GestionForm
import cl.zzenner.cobranza.feature.gestion.domain.GestionValidator
import cl.zzenner.cobranza.feature.gestion.domain.OrigenGestion
import cl.zzenner.cobranza.feature.gestion.domain.TipoGestion
import cl.zzenner.cobranza.feature.gestion.domain.UbicacionCapturada
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestionValidatorTest {

    private val ubicacion = UbicacionCapturada(
        latitud = -33.45,
        longitud = -70.66,
        precisionMetros = 5.0f,
        ubicacionSimulada = false,
        proveedorGps = "gps",
        fechaCapturaEpoch = 1000L,
    )

    private fun form(
        tipo: TipoGestion = TipoGestion.SIN_CONTACTO,
        observacion: String? = null,
        observacionDireccion: String? = null,
        fechaCompromiso: String? = null,
    ) = GestionForm(
        personaId = "p-1",
        personaRutNumero = "12345678",
        personaRutDv = "9",
        personaNombre = "Test",
        origenGestion = OrigenGestion.ASIGNACION_DIARIA,
        asignacionDiariaId = "asig-1",
        tipoGestion = tipo,
        observacion = observacion,
        observacionDireccion = observacionDireccion,
        fechaCompromiso = fechaCompromiso,
        ubicacion = ubicacion,
    )

    @Test
    fun `SIN_CONTACTO sin observacion es valido`() {
        val errores = GestionValidator.validar(form(), "2026-08-02")
        assertTrue(errores.isEmpty())
    }

    @Test
    fun `COMPROMISO_PAGO sin fecha es invalido`() {
        val errores = GestionValidator.validar(form(tipo = TipoGestion.COMPROMISO_PAGO), "2026-08-02")
        assertTrue(errores.any { it is ErrorValidacion.FechaCompromisoRequerida })
    }

    @Test
    fun `COMPROMISO_PAGO con fecha futura es valido`() {
        val errores = GestionValidator.validar(
            form(tipo = TipoGestion.COMPROMISO_PAGO, fechaCompromiso = "2026-12-31"),
            "2026-08-02",
        )
        assertTrue(errores.isEmpty())
    }

    @Test
    fun `COMPROMISO_PAGO con fecha pasada es invalido`() {
        val errores = GestionValidator.validar(
            form(tipo = TipoGestion.COMPROMISO_PAGO, fechaCompromiso = "2026-01-01"),
            "2026-08-02",
        )
        assertTrue(errores.any { it is ErrorValidacion.FechaCompromisoInvalida })
    }

    @Test
    fun `COMPROMISO_PAGO con fecha igual a hoy es valido`() {
        val errores = GestionValidator.validar(
            form(tipo = TipoGestion.COMPROMISO_PAGO, fechaCompromiso = "2026-08-02"),
            "2026-08-02",
        )
        assertFalse(errores.any { it is ErrorValidacion.FechaCompromisoInvalida })
    }

    @Test
    fun `observacion mayor a 500 caracteres es invalida`() {
        val obs = "x".repeat(501)
        val errores = GestionValidator.validar(form(observacion = obs), "2026-08-02")
        assertTrue(errores.any { it is ErrorValidacion.ObservacionDemasiadoLarga })
    }

    @Test
    fun `observacion de exactamente 500 caracteres es valida`() {
        val obs = "x".repeat(500)
        val errores = GestionValidator.validar(form(observacion = obs), "2026-08-02")
        assertFalse(errores.any { it is ErrorValidacion.ObservacionDemasiadoLarga })
    }

    @Test
    fun `observacion direccion mayor a 200 caracteres es invalida`() {
        val obs = "x".repeat(201)
        val errores = GestionValidator.validar(form(observacionDireccion = obs), "2026-08-02")
        assertTrue(errores.any { it is ErrorValidacion.ObservacionDireccionDemasiadoLarga })
    }
}
