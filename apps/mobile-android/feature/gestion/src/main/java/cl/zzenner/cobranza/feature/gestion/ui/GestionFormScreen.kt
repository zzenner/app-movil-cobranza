package cl.zzenner.cobranza.feature.gestion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.zzenner.cobranza.feature.gestion.domain.ErrorValidacion
import cl.zzenner.cobranza.feature.gestion.domain.TipoGestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: GestionFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.guardadoExitoso) {
        if (state.guardadoExitoso) onNavigateBack()
    }

    LaunchedEffect(state.errorGeneral) {
        val error = state.errorGeneral
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.limpiarError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar gestión") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SeccionTipoGestion(
                seleccionado = state.tipoGestion,
                onSeleccionar = viewModel::onTipoGestionChanged,
                tieneError = state.errores.any { it is ErrorValidacion.TipoGestionRequerido },
            )

            SeccionGps(
                gpsState = state.gpsState,
                onCapturar = viewModel::capturarUbicacion,
                tieneError = state.errores.any { it is ErrorValidacion.UbicacionRequerida },
            )

            OutlinedTextField(
                value = state.observacion,
                onValueChange = viewModel::onObservacionChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Observación (opcional)") },
                maxLines = 4,
                isError = state.errores.any { it is ErrorValidacion.ObservacionDemasiadoLarga },
                supportingText = { Text("${state.observacion.length}/500") },
            )

            OutlinedTextField(
                value = state.observacionDireccion,
                onValueChange = viewModel::onObservacionDireccionChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Observación de dirección (opcional)") },
                maxLines = 2,
                isError = state.errores.any { it is ErrorValidacion.ObservacionDireccionDemasiadoLarga },
                supportingText = { Text("${state.observacionDireccion.length}/200") },
            )

            if (state.tipoGestion == TipoGestion.COMPROMISO_PAGO) {
                OutlinedTextField(
                    value = state.fechaCompromiso,
                    onValueChange = viewModel::onFechaCompromisoChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Fecha de compromiso (YYYY-MM-DD)") },
                    singleLine = true,
                    isError = state.errores.any {
                        it is ErrorValidacion.FechaCompromisoRequerida ||
                            it is ErrorValidacion.FechaCompromisoInvalida
                    },
                    supportingText = {
                        when {
                            state.errores.any { it is ErrorValidacion.FechaCompromisoRequerida } ->
                                Text("Fecha requerida para compromiso de pago")
                            state.errores.any { it is ErrorValidacion.FechaCompromisoInvalida } ->
                                Text("La fecha debe ser hoy o posterior")
                            else -> {}
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = viewModel::guardar,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting && state.gpsState is GpsState.Capturado,
            ) {
                Text(if (state.isSubmitting) "Guardando..." else "Registrar gestión")
            }
        }
    }
}

@Composable
private fun SeccionTipoGestion(
    seleccionado: TipoGestion?,
    onSeleccionar: (TipoGestion) -> Unit,
    tieneError: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Tipo de gestión",
            style = MaterialTheme.typography.titleSmall,
            color = if (tieneError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TipoGestion.entries.forEach { tipo ->
                FilterChip(
                    selected = seleccionado == tipo,
                    onClick = { onSeleccionar(tipo) },
                    label = { Text(tipo.etiqueta()) },
                )
            }
        }
        if (tieneError) {
            Text(
                "Seleccione un tipo de gestión",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SeccionGps(
    gpsState: GpsState,
    onCapturar: () -> Unit,
    tieneError: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                tieneError -> MaterialTheme.colorScheme.errorContainer
                gpsState is GpsState.Capturado -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                when (gpsState) {
                    GpsState.Idle -> {
                        Text("Ubicación GPS", style = MaterialTheme.typography.titleSmall)
                        Text("Requerida para registrar", style = MaterialTheme.typography.bodySmall)
                    }
                    GpsState.Capturando -> {
                        Text("Capturando GPS...", style = MaterialTheme.typography.titleSmall)
                        Text("Por favor espere", style = MaterialTheme.typography.bodySmall)
                    }
                    is GpsState.Capturado -> {
                        val ub = gpsState.ubicacion
                        Text("GPS capturado", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "±${ub.precisionMetros.toInt()} m — ${ub.latitud.format()}, ${ub.longitud.format()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (ub.ubicacionSimulada) {
                            Text(
                                "⚠ Ubicación simulada",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    is GpsState.Error -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                gpsState.mensaje,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onCapturar,
                enabled = gpsState !is GpsState.Capturando,
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Text(if (gpsState is GpsState.Capturado) "Recapturar" else "Capturar")
            }
        }
    }
}

private fun TipoGestion.etiqueta(): String = when (this) {
    TipoGestion.SIN_CONTACTO -> "Sin contacto"
    TipoGestion.CONTACTO_FAMILIAR -> "Contacto familiar"
    TipoGestion.COMPROMISO_PAGO -> "Compromiso pago"
}

private fun Double.format() = "%.5f".format(this)
