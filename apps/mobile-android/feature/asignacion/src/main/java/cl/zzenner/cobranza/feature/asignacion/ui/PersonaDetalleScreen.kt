package cl.zzenner.cobranza.feature.asignacion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.zzenner.cobranza.core.database.dao.OperacionConCuotas
import cl.zzenner.cobranza.core.database.dao.PersonaConDetalle
import cl.zzenner.cobranza.core.database.entity.DireccionEntity
import cl.zzenner.cobranza.core.database.entity.GestionHistoricaEntity
import cl.zzenner.cobranza.core.database.entity.GestionLocalEntity
import cl.zzenner.cobranza.feature.asignacion.domain.formatearRut
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaDetalleScreen(
    personaId: String,
    asignacionDiariaId: String,
    onNavigateBack: () -> Unit,
    onRegistrarGestion: () -> Unit,
    onVerHistorial: () -> Unit,
    viewModel: PersonaDetalleViewModel = hiltViewModel(),
) {
    val personaConDetalle by viewModel.personaConDetalle.collectAsState(initial = null)
    val operaciones by viewModel.operacionesConCuotas.collectAsState(initial = emptyList())
    val gestiones by viewModel.gestionesHistoricas.collectAsState(initial = emptyList())
    val gestionesLocales by viewModel.gestionesLocales.collectAsState(initial = emptyList())

    val persona = personaConDetalle

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(persona?.persona?.nombre ?: "Detalle") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        if (persona == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Cargando datos...")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Datos personales
                SeccionDatosPersona(persona)
                HorizontalDivider()

                // Direcciones vigentes
                SeccionDirecciones(persona.direcciones.filter { it.vigente })
                HorizontalDivider()

                // Avales
                if (persona.avales.isNotEmpty()) {
                    SeccionAvales(persona)
                    HorizontalDivider()
                }

                // Operaciones con cuotas
                SeccionOperaciones(operaciones)
                HorizontalDivider()

                // Gestiones históricas (readonly)
                SeccionGestiones(gestiones)
                HorizontalDivider()

                // Gestiones locales pendientes de sincronizar
                SeccionGestionesLocales(
                    gestionesLocales = gestionesLocales,
                    onRegistrar = onRegistrarGestion,
                    onVerHistorial = onVerHistorial,
                )
            }
        }
    }
}

@Composable
private fun SeccionDatosPersona(persona: PersonaConDetalle) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Datos personales", style = MaterialTheme.typography.titleMedium)
        Text("Nombre: ${persona.persona.nombre}")
        Text("RUT: ${formatearRut(persona.persona.rutNumero, persona.persona.rutDv)}")
    }
}

@Composable
private fun SeccionDirecciones(direcciones: List<DireccionEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Direcciones vigentes (${direcciones.size})",
            style = MaterialTheme.typography.titleMedium,
        )
        if (direcciones.isEmpty()) {
            Text("Sin direcciones registradas", style = MaterialTheme.typography.bodySmall)
        }
        direcciones.forEach { dir ->
            Text("${dir.tipo}: ${dir.texto}" + if (dir.comuna != null) ", ${dir.comuna}" else "")
        }
    }
}

@Composable
private fun SeccionAvales(persona: PersonaConDetalle) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Avales (${persona.avales.size})",
            style = MaterialTheme.typography.titleMedium,
        )
        persona.avales.forEach { aval ->
            Text("${aval.nombre} — RUT: ${formatearRut(aval.rutNumero, aval.rutDv)}")
        }
    }
}

@Composable
private fun SeccionOperaciones(operaciones: List<OperacionConCuotas>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Operaciones (${operaciones.size})",
            style = MaterialTheme.typography.titleMedium,
        )
        if (operaciones.isEmpty()) {
            Text("Sin operaciones", style = MaterialTheme.typography.bodySmall)
        }
        operaciones.forEach { opConCuotas ->
            val op = opConCuotas.operacion
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("N° ${op.numeroOperacion} — ${op.estado}", style = MaterialTheme.typography.titleSmall)
                    Text("Capital: \$${BigDecimal(op.capital).toPlainString()}")
                    Text("Total vigente: \$${BigDecimal(op.totalVigente).toPlainString()}")
                    if (opConCuotas.cuotas.isNotEmpty()) {
                        Text(
                            "Cuotas (${opConCuotas.cuotas.size})",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        opConCuotas.cuotas.take(3).forEach { cuota ->
                            Text(
                                "  N°${cuota.numeroCuota} — ${cuota.estado} — Saldo: \$${BigDecimal(cuota.saldo).toPlainString()} — Vcto: ${cuota.fechaVencimiento}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (opConCuotas.cuotas.size > 3) {
                            Text(
                                "  ... y ${opConCuotas.cuotas.size - 3} más",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionGestionesLocales(
    gestionesLocales: List<GestionLocalEntity>,
    onRegistrar: () -> Unit,
    onVerHistorial: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Gestiones en este dispositivo", style = MaterialTheme.typography.titleMedium)
            if (gestionesLocales.isNotEmpty()) {
                TextButton(onClick = onVerHistorial) { Text("Ver todas") }
            }
        }
        if (gestionesLocales.isEmpty()) {
            Text("Sin gestiones registradas en esta sesión", style = MaterialTheme.typography.bodySmall)
        } else {
            val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT)
            gestionesLocales.take(3).forEach { g ->
                Text(
                    "${g.tipoGestion} — ${formato.format(Date(g.fechaGestionEpoch))} — ${g.estadoSincronizacion}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Button(onClick = onRegistrar, modifier = Modifier.fillMaxWidth()) {
            Text("Registrar gestión")
        }
    }
}

@Composable
private fun SeccionGestiones(gestiones: List<GestionHistoricaEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Últimas gestiones (${gestiones.size})",
            style = MaterialTheme.typography.titleMedium,
        )
        if (gestiones.isEmpty()) {
            Text("Sin gestiones registradas", style = MaterialTheme.typography.bodySmall)
        }
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT)
        gestiones.take(5).forEach { gestion ->
            Text(
                "${gestion.tipoGestion} — ${formato.format(Date(gestion.fechaGestion))}" +
                    if (gestion.observacion != null) "\n${gestion.observacion}" else "",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
