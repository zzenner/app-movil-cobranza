package cl.zzenner.cobranza.core.database.transaction

import cl.zzenner.cobranza.core.database.entity.AsignacionDiariaEntity
import cl.zzenner.cobranza.core.database.entity.AsignacionPersonaCrossRef
import cl.zzenner.cobranza.core.database.entity.AvalEntity
import cl.zzenner.cobranza.core.database.entity.CuotaEntity
import cl.zzenner.cobranza.core.database.entity.DireccionEntity
import cl.zzenner.cobranza.core.database.entity.GestionHistoricaEntity
import cl.zzenner.cobranza.core.database.entity.OperacionEntity
import cl.zzenner.cobranza.core.database.entity.PersonaEntity

/**
 * Bundle completo de datos ya mapeados de DTOs a entidades Room,
 * listo para ser persistido por [BundleReplacementTransaction].
 * No contiene DTOs de red ni lógica de conversión.
 */
data class BundleDescargado(
    val asignacion: AsignacionDiariaEntity,
    val personas: List<PersonaEntity>,
    val crossRefs: List<AsignacionPersonaCrossRef>,
    val direcciones: List<DireccionEntity>,
    val avales: List<AvalEntity>,
    val operaciones: List<OperacionEntity>,
    val cuotas: List<CuotaEntity>,
    val gestionesHistoricas: List<GestionHistoricaEntity>,
)
