package cl.zzenner.cobranza.feature.asignacion

import cl.zzenner.cobranza.core.network.api.AvalDescargaDto
import cl.zzenner.cobranza.core.network.api.CuotaDescargaDto
import cl.zzenner.cobranza.core.network.api.DireccionDescargaDto
import cl.zzenner.cobranza.core.network.api.GestionDescargaDto
import cl.zzenner.cobranza.core.network.api.OperacionDescargaDto
import cl.zzenner.cobranza.core.network.api.PersonaDescargaDto
import cl.zzenner.cobranza.core.network.api.RespuestaAsignacionDiariaDto
import cl.zzenner.cobranza.feature.asignacion.data.AsignacionMapper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class AsignacionMapperTest {

    private val mapper = AsignacionMapper()

    private fun dtoCompleto() = RespuestaAsignacionDiariaDto(
        id = "asig-001",
        ejecutivoId = "eje-001",
        fecha = "2026-08-02",
        estado = "PUBLICADA",
        personas = listOf(
            PersonaDescargaDto(
                id = "p-001",
                rutNumero = "27000001",
                rutDv = "0",
                nombre = "Persona Test",
                direcciones = listOf(
                    DireccionDescargaDto(
                        personaId = "p-001",
                        tipo = "DOMICILIO",
                        texto = "Av. Principal 123",
                        comuna = "Santiago",
                        ciudad = "Santiago",
                        vigente = true,
                    ),
                ),
                avales = listOf(
                    AvalDescargaDto(
                        personaId = "p-001",
                        rutNumero = "11111111",
                        rutDv = "1",
                        nombre = "Aval Test",
                    ),
                ),
                operaciones = listOf(
                    OperacionDescargaDto(
                        id = "op-001",
                        personaId = "p-001",
                        numeroOperacion = "OP001",
                        estado = "VIGENTE",
                        capital = BigDecimal("500000.00"),
                        interesPenal = BigDecimal("1234.56"),
                        gastosCobranza = BigDecimal("0"),
                        totalVigente = BigDecimal("501234.56"),
                        cuotas = listOf(
                            CuotaDescargaDto(
                                numeroCuota = 1,
                                estado = "VENCIDA",
                                montoTotal = BigDecimal("50000.00"),
                                saldo = BigDecimal("50000.00"),
                                fechaVencimiento = "2026-07-01",
                            ),
                        ),
                    ),
                ),
                ultimasGestiones = listOf(
                    GestionDescargaDto(
                        id = "g-001",
                        origenGestion = "ASIGNACION_DIARIA",
                        asignacionDiariaId = "asig-001",
                        personaId = "p-001",
                        ejecutivoId = "eje-001",
                        tipoGestion = "SIN_CONTACTO",
                        fechaGestion = "2026-08-01T10:30:00Z",
                        observacion = "Sin respuesta",
                        observacionDireccion = null,
                        latitud = -33.45,
                        longitud = -70.66,
                        precisionMetros = 10.0f,
                        proveedorGps = null,
                        ubicacionSimulada = false,
                        fechaCapturaGps = "2026-08-01T10:30:00Z",
                        fechaCompromiso = null,
                        fechaCreacionServidor = "2026-08-01T10:31:00Z",
                    ),
                ),
            ),
        ),
    )

    @Test
    fun `dto completo se mapea a BundleDescargado con todos los campos`() {
        val bundle = mapper.map(dtoCompleto(), 12345L)

        assertEquals("asig-001", bundle.asignacion.id)
        assertEquals("2026-08-02", bundle.asignacion.fecha)
        assertEquals(12345L, bundle.asignacion.fechaDescargaEpoch)
        assertEquals(1, bundle.personas.size)
        assertEquals("p-001", bundle.personas[0].id)
        assertEquals(1, bundle.crossRefs.size)
        assertEquals("asig-001", bundle.crossRefs[0].asignacionId)
        assertEquals(1, bundle.direcciones.size)
        assertEquals(1, bundle.avales.size)
        assertEquals(1, bundle.operaciones.size)
        assertEquals(1, bundle.cuotas.size)
        assertEquals(1, bundle.gestionesHistoricas.size)
    }

    @Test
    fun `BigDecimal mapeado sin perdida de precision`() {
        val bundle = mapper.map(dtoCompleto(), 0L)
        val op = bundle.operaciones.first()

        assertEquals("500000.00", op.capital)
        assertEquals("1234.56", op.interesPenal)
        assertEquals("0", op.gastosCobranza)
        assertEquals("501234.56", op.totalVigente)
    }

    @Test
    fun `fechas Instant mapeadas a epoch millis`() {
        val bundle = mapper.map(dtoCompleto(), 0L)
        val gestion = bundle.gestionesHistoricas.first()

        // "2026-08-01T10:30:00Z" → epoch millis > 0
        assert(gestion.fechaGestion > 0L)
        assert(gestion.fechaCapturaGps > 0L)
        assert(gestion.fechaCreacionServidor >= gestion.fechaGestion)
    }

    @Test
    fun `fecha vencimiento cuota preservada como string ISO`() {
        val bundle = mapper.map(dtoCompleto(), 0L)
        assertEquals("2026-07-01", bundle.cuotas.first().fechaVencimiento)
    }

    @Test
    fun `aval tiene personaIdTitular correcto`() {
        val bundle = mapper.map(dtoCompleto(), 0L)
        assertEquals("p-001", bundle.avales.first().personaIdTitular)
    }

    @Test
    fun `gestion con asignacionDiariaId null se mapea correctamente`() {
        val dto = dtoCompleto().copy(
            personas = listOf(
                dtoCompleto().personas[0].copy(
                    ultimasGestiones = listOf(
                        dtoCompleto().personas[0].ultimasGestiones[0].copy(
                            asignacionDiariaId = null,
                            origenGestion = "LIBRE",
                        ),
                    ),
                ),
            ),
        )
        val bundle = mapper.map(dto, 0L)
        assertEquals(null, bundle.gestionesHistoricas.first().asignacionDiariaId)
    }
}
