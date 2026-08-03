package cl.zzenner.cobranza.feature.gestion

import cl.zzenner.cobranza.core.database.entity.GestionHistoricaEntity
import cl.zzenner.cobranza.feature.gestion.data.GestionMapper
import cl.zzenner.cobranza.feature.gestion.domain.EstadoSincronizacion
import cl.zzenner.cobranza.feature.gestion.domain.GestionForm
import cl.zzenner.cobranza.feature.gestion.domain.TipoGestion
import cl.zzenner.cobranza.feature.gestion.domain.UbicacionCapturada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class GestionMapperTest {

    private val ubicacion = UbicacionCapturada(
        latitud = -33.45,
        longitud = -70.66,
        precisionMetros = 12.5f,
        ubicacionSimulada = false,
        proveedorGps = "gps",
        fechaCapturaEpoch = 1_000_000L,
    )

    private val form = GestionForm(
        personaId = "persona-uuid",
        personaRutNumero = "27000001",
        personaRutDv = "0",
        personaNombre = "Juan Pérez",
        asignacionDiariaId = "asig-uuid",
        tipoGestion = TipoGestion.SIN_CONTACTO,
        observacion = "Nadie en casa",
        observacionDireccion = null,
        fechaCompromiso = null,
        ubicacion = ubicacion,
    )

    @Test
    fun `formToEntity preserva todos los campos`() {
        val id = UUID.randomUUID()
        val ahora = 2_000_000L
        val entity = GestionMapper.formToEntity(form, id, ahora)

        assertEquals(id.toString(), entity.id)
        assertEquals("persona-uuid", entity.personaId)
        assertEquals("27000001", entity.personaRutNumero)
        assertEquals("Juan Pérez", entity.personaNombre)
        assertEquals("SIN_CONTACTO", entity.tipoGestion)
        assertEquals("ASIGNACION_DIARIA", entity.origenGestion)
        assertEquals(ahora, entity.fechaGestionEpoch)
        assertEquals(1_000_000L, entity.fechaCapturaGpsEpoch)
        assertEquals("Nadie en casa", entity.observacion)
        assertEquals(-33.45, entity.latitud, 0.0001)
        assertEquals(12.5f, entity.precisionMetros)
        assertFalse(entity.ubicacionSimulada)
        assertEquals(EstadoSincronizacion.PENDIENTE_ENVIO.name, entity.estadoSincronizacion)
        assertEquals(0, entity.cantidadIntentos)
        assertNull(entity.leaseHastaEpoch)
        assertNull(entity.fechaProximoIntentoEpoch)
    }

    @Test
    fun `entityToDto convierte precisionMetros de Float a Double`() {
        val id = UUID.randomUUID()
        val entity = GestionMapper.formToEntity(form, id, 2_000_000L)
        val dto = GestionMapper.entityToDto(entity)

        assertEquals(12.5, dto.precisionMetros, 0.001)
        assertTrue(dto.precisionMetros is Double)
    }

    @Test
    fun `entityToDto produce fechaGestion en formato ISO-8601`() {
        val id = UUID.randomUUID()
        val entity = GestionMapper.formToEntity(form, id, 0L)
        val dto = GestionMapper.entityToDto(entity)

        assertTrue("fecha debe ser ISO-8601", dto.fechaGestion.contains("T"))
        assertTrue("fecha debe terminar en Z", dto.fechaGestion.endsWith("Z"))
    }

    @Test
    fun `historicaEntityToResumen marca esLocal=false y estadoSINRONIZADA`() {
        val historica = GestionHistoricaEntity(
            id = "h-1",
            personaId = "p-1",
            asignacionDiariaId = "a-1",
            ejecutivoId = "e-1",
            origenGestion = "ASIGNACION_DIARIA",
            tipoGestion = "COMPROMISO_PAGO",
            fechaGestion = 1000L,
            observacion = null,
            observacionDireccion = null,
            latitud = -33.0,
            longitud = -70.0,
            precisionMetros = 5f,
            proveedorGps = null,
            ubicacionSimulada = false,
            fechaCapturaGps = 1000L,
            fechaCompromiso = "2026-09-01",
            fechaCreacionServidor = 1000L,
        )

        val resumen = GestionMapper.historicaEntityToResumen(historica)

        assertFalse(resumen.esLocal)
        assertEquals(EstadoSincronizacion.SINCRONIZADA, resumen.estadoSincronizacion)
        assertEquals("2026-09-01", resumen.fechaCompromiso)
        assertEquals(TipoGestion.COMPROMISO_PAGO, resumen.tipoGestion)
    }

    @Test
    fun `localEntityToResumen marca esLocal=true`() {
        val id = UUID.randomUUID()
        val entity = GestionMapper.formToEntity(form, id, 1000L)
        val resumen = GestionMapper.localEntityToResumen(entity)

        assertTrue(resumen.esLocal)
        assertEquals(EstadoSincronizacion.PENDIENTE_ENVIO, resumen.estadoSincronizacion)
    }
}
