package cl.zzenner.cobranza.feature.busqueda

import cl.zzenner.cobranza.core.database.dao.PersonaDirectaDao
import cl.zzenner.cobranza.core.database.entity.PersonaDirectaEntity
import cl.zzenner.cobranza.core.network.api.PersonaBusquedaApi
import cl.zzenner.cobranza.core.network.api.PersonaDescargaDto
import cl.zzenner.cobranza.core.network.api.SolicitudBusquedaDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

sealed class ResultadoBusqueda {
    data class Encontrada(val personaId: String) : ResultadoBusqueda()
    data object NoEncontrada : ResultadoBusqueda()
    data object RutInvalido : ResultadoBusqueda()
    data object SinConexion : ResultadoBusqueda()
    data class Error(val mensaje: String?) : ResultadoBusqueda()
}

class BusquedaDirectaRepository @Inject constructor(
    private val api: PersonaBusquedaApi,
    private val dao: PersonaDirectaDao,
    private val json: Json,
) {

    suspend fun buscar(rutNumero: String, rutDv: String): ResultadoBusqueda {
        if (!RutValidator.esValido(rutNumero, rutDv)) {
            return ResultadoBusqueda.RutInvalido
        }

        return try {
            val response = api.buscarPersona(
                SolicitudBusquedaDto(
                    rutNumero = rutNumero.trim(),
                    rutDv = rutDv.trim().uppercase(),
                ),
            )

            when {
                response.isSuccessful -> {
                    val body = response.body() ?: return ResultadoBusqueda.Error("Respuesta vacía del servidor")
                    val generadoEnEpoch = runCatching {
                        Instant.parse(body.generadoEn).toEpochMilli()
                    }.getOrElse { System.currentTimeMillis() }

                    val detalleJson = json.encodeToString(body.persona)
                    val entity = PersonaDirectaEntity(
                        id = body.persona.id,
                        rutNumero = body.persona.rutNumero,
                        rutDv = body.persona.rutDv,
                        nombre = body.persona.nombre,
                        versionContrato = body.version,
                        generadoEnEpoch = generadoEnEpoch,
                        fechaConsultaEpoch = System.currentTimeMillis(),
                        detalleJson = detalleJson,
                    )
                    dao.upsert(entity)
                    ResultadoBusqueda.Encontrada(body.persona.id)
                }
                response.code() == 404 -> ResultadoBusqueda.NoEncontrada
                response.code() == 400 -> {
                    val body = response.errorBody()?.string() ?: ""
                    if (body.contains("RUT_INVALIDO")) ResultadoBusqueda.RutInvalido
                    else ResultadoBusqueda.Error("Solicitud inválida")
                }
                else -> ResultadoBusqueda.Error("Error del servidor: ${response.code()}")
            }
        } catch (e: IOException) {
            ResultadoBusqueda.SinConexion
        } catch (e: Exception) {
            ResultadoBusqueda.Error(e.message)
        }
    }
}
