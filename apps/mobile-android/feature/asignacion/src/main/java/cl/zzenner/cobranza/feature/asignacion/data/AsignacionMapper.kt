package cl.zzenner.cobranza.feature.asignacion.data

import cl.zzenner.cobranza.core.database.entity.AsignacionDiariaEntity
import cl.zzenner.cobranza.core.database.entity.AsignacionPersonaCrossRef
import cl.zzenner.cobranza.core.database.entity.AvalEntity
import cl.zzenner.cobranza.core.database.entity.CuotaEntity
import cl.zzenner.cobranza.core.database.entity.DireccionEntity
import cl.zzenner.cobranza.core.database.entity.GestionHistoricaEntity
import cl.zzenner.cobranza.core.database.entity.OperacionEntity
import cl.zzenner.cobranza.core.database.entity.PersonaEntity
import cl.zzenner.cobranza.core.database.transaction.BundleDescargado
import cl.zzenner.cobranza.core.network.api.GestionDescargaDto
import cl.zzenner.cobranza.core.network.api.PersonaDescargaDto
import cl.zzenner.cobranza.core.network.api.RespuestaAsignacionDiariaDto
import java.time.Instant
import javax.inject.Inject

/**
 * Convierte [RespuestaAsignacionDiariaDto] (DTOs de red) en [BundleDescargado] (entidades Room).
 *
 * Reglas de conversión:
 * - UUID (String) → String (sin transformación)
 * - LocalDate String → String (ISO ya correcto)
 * - Instant String → Long (toEpochMilli via Instant.parse)
 * - BigDecimal → String via toPlainString (nunca Double)
 */
class AsignacionMapper @Inject constructor() {

    fun map(dto: RespuestaAsignacionDiariaDto, fechaDescargaEpoch: Long): BundleDescargado {
        val asignacion = AsignacionDiariaEntity(
            id = dto.id,
            ejecutivoId = dto.ejecutivoId,
            fecha = dto.fecha,
            estado = dto.estado,
            fechaPublicacion = null, // no viene en el bundle
            fechaDescargaEpoch = fechaDescargaEpoch,
        )

        val personas = dto.personas.map { mapPersona(it) }
        val crossRefs = dto.personas.map { AsignacionPersonaCrossRef(dto.id, it.id) }
        val direcciones = dto.personas.flatMap { persona ->
            persona.direcciones.map { dir ->
                DireccionEntity(
                    personaId = persona.id,
                    tipo = dir.tipo,
                    texto = dir.texto,
                    comuna = dir.comuna,
                    ciudad = dir.ciudad,
                    vigente = dir.vigente,
                )
            }
        }
        val avales = dto.personas.flatMap { persona ->
            persona.avales.map { aval ->
                AvalEntity(
                    personaIdTitular = persona.id,
                    rutNumero = aval.rutNumero,
                    rutDv = aval.rutDv,
                    nombre = aval.nombre,
                )
            }
        }
        val operaciones = dto.personas.flatMap { persona ->
            persona.operaciones.map { op ->
                OperacionEntity(
                    id = op.id,
                    personaId = op.personaId,
                    numeroOperacion = op.numeroOperacion,
                    estado = op.estado,
                    capital = op.capital.toPlainString(),
                    interesPenal = op.interesPenal.toPlainString(),
                    gastosCobranza = op.gastosCobranza.toPlainString(),
                    totalVigente = op.totalVigente.toPlainString(),
                )
            }
        }
        val cuotas = dto.personas.flatMap { persona ->
            persona.operaciones.flatMap { op ->
                op.cuotas.map { cuota ->
                    CuotaEntity(
                        operacionId = op.id,
                        numeroCuota = cuota.numeroCuota,
                        estado = cuota.estado,
                        montoTotal = cuota.montoTotal.toPlainString(),
                        saldo = cuota.saldo.toPlainString(),
                        fechaVencimiento = cuota.fechaVencimiento,
                    )
                }
            }
        }
        val gestiones = dto.personas.flatMap { persona ->
            persona.ultimasGestiones.map { mapGestion(it) }
        }

        return BundleDescargado(
            asignacion = asignacion,
            personas = personas,
            crossRefs = crossRefs,
            direcciones = direcciones,
            avales = avales,
            operaciones = operaciones,
            cuotas = cuotas,
            gestionesHistoricas = gestiones,
        )
    }

    private fun mapPersona(dto: PersonaDescargaDto): PersonaEntity =
        PersonaEntity(
            id = dto.id,
            rutNumero = dto.rutNumero,
            rutDv = dto.rutDv,
            nombre = dto.nombre,
        )

    private fun mapGestion(dto: GestionDescargaDto): GestionHistoricaEntity =
        GestionHistoricaEntity(
            id = dto.id,
            personaId = dto.personaId,
            asignacionDiariaId = dto.asignacionDiariaId,
            ejecutivoId = dto.ejecutivoId,
            origenGestion = dto.origenGestion,
            tipoGestion = dto.tipoGestion,
            fechaGestion = Instant.parse(dto.fechaGestion).toEpochMilli(),
            observacion = dto.observacion,
            observacionDireccion = dto.observacionDireccion,
            latitud = dto.latitud,
            longitud = dto.longitud,
            precisionMetros = dto.precisionMetros,
            proveedorGps = dto.proveedorGps,
            ubicacionSimulada = dto.ubicacionSimulada,
            fechaCapturaGps = Instant.parse(dto.fechaCapturaGps).toEpochMilli(),
            fechaCompromiso = dto.fechaCompromiso,
            fechaCreacionServidor = Instant.parse(dto.fechaCreacionServidor).toEpochMilli(),
        )
}
