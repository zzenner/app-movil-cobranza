package cl.zzenner.cobranza.feature.gestion.data

import cl.zzenner.cobranza.core.database.entity.GestionHistoricaEntity
import cl.zzenner.cobranza.core.database.entity.GestionLocalEntity
import cl.zzenner.cobranza.core.network.api.SolicitudGestionDto
import cl.zzenner.cobranza.feature.gestion.domain.EstadoSincronizacion
import cl.zzenner.cobranza.feature.gestion.domain.GestionForm
import cl.zzenner.cobranza.feature.gestion.domain.GestionResumen
import cl.zzenner.cobranza.feature.gestion.domain.TipoGestion
import java.time.Instant
import java.util.UUID

object GestionMapper {

    fun formToEntity(form: GestionForm, id: UUID, ahora: Long): GestionLocalEntity =
        GestionLocalEntity(
            id = id.toString(),
            personaId = form.personaId,
            personaRutNumero = form.personaRutNumero,
            personaRutDv = form.personaRutDv,
            personaNombre = form.personaNombre,
            asignacionDiariaId = form.asignacionDiariaId,
            origenGestion = "ASIGNACION_DIARIA",
            tipoGestion = form.tipoGestion.name,
            fechaGestionEpoch = ahora,
            fechaCapturaGpsEpoch = form.ubicacion.fechaCapturaEpoch,
            observacion = form.observacion,
            observacionDireccion = form.observacionDireccion,
            latitud = form.ubicacion.latitud,
            longitud = form.ubicacion.longitud,
            precisionMetros = form.ubicacion.precisionMetros,
            ubicacionSimulada = form.ubicacion.ubicacionSimulada,
            proveedorGps = form.ubicacion.proveedorGps,
            fechaCompromiso = form.fechaCompromiso,
            estadoSincronizacion = EstadoSincronizacion.PENDIENTE_ENVIO.name,
            fechaCreacionLocalEpoch = ahora,
            cantidadIntentos = 0,
            leaseHastaEpoch = null,
            fechaProximoIntentoEpoch = null,
            codigoErrorServidor = null,
            mensajeError = null,
        )

    fun entityToDto(entity: GestionLocalEntity): SolicitudGestionDto =
        SolicitudGestionDto(
            id = entity.id,
            personaId = entity.personaId,
            origenGestion = entity.origenGestion,
            asignacionDiariaId = entity.asignacionDiariaId,
            tipoGestion = entity.tipoGestion,
            fechaGestion = Instant.ofEpochMilli(entity.fechaGestionEpoch).toString(),
            fechaCapturaGps = Instant.ofEpochMilli(entity.fechaCapturaGpsEpoch).toString(),
            observacion = entity.observacion,
            observacionDireccion = entity.observacionDireccion,
            latitud = entity.latitud,
            longitud = entity.longitud,
            precisionMetros = entity.precisionMetros.toDouble(),
            ubicacionSimulada = entity.ubicacionSimulada,
            proveedorGps = entity.proveedorGps,
            fechaCompromiso = entity.fechaCompromiso,
        )

    fun localEntityToResumen(entity: GestionLocalEntity): GestionResumen =
        GestionResumen(
            id = entity.id,
            tipoGestion = TipoGestion.valueOf(entity.tipoGestion),
            fechaGestionEpoch = entity.fechaGestionEpoch,
            observacion = entity.observacion,
            estadoSincronizacion = EstadoSincronizacion.valueOf(entity.estadoSincronizacion),
            fechaCompromiso = entity.fechaCompromiso,
            esLocal = true,
        )

    fun historicaEntityToResumen(entity: GestionHistoricaEntity): GestionResumen =
        GestionResumen(
            id = entity.id,
            tipoGestion = TipoGestion.valueOf(entity.tipoGestion),
            fechaGestionEpoch = entity.fechaGestion,
            observacion = entity.observacion,
            estadoSincronizacion = EstadoSincronizacion.SINCRONIZADA,
            fechaCompromiso = entity.fechaCompromiso,
            esLocal = false,
        )
}
