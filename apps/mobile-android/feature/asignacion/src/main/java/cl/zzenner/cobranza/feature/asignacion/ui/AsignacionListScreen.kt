package cl.zzenner.cobranza.feature.asignacion.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.zzenner.cobranza.feature.asignacion.domain.PersonaResumen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsignacionListScreen(
    onNavigateToDetalle: (personaId: String, asignacionDiariaId: String) -> Unit,
    viewModel: AsignacionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val meta = state.syncMetadata

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Asignación") },
                actions = {
                    IconButton(onClick = viewModel::sincronizarManual) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sincronizar")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Banner de estado
            when {
                meta.datosMarcadosComoDesactualizados && meta.estado == "SIN_ASIGNACION" -> {
                    BannerDesactualizado(meta.fechaAsignacionAlmacenada)
                }
                meta.estado == "SIN_ASIGNACION" && !meta.datosAnterioresDisponibles -> {
                    BannerSinAsignacion()
                }
            }

            // Barra de búsqueda
            OutlinedTextField(
                value = state.textoBusqueda,
                onValueChange = viewModel::onBusquedaChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar por RUT o nombre") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )

            // Última sincronización
            if (meta.ultimaDescargaFormateada != null) {
                Text(
                    text = "Última sincronización: ${meta.ultimaDescargaFormateada}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            if (state.cargando && state.personas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Cargando...")
                }
            } else if (state.personas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin personas en la asignación")
                }
            } else {
                LazyColumn {
                    items(state.personas, key = { it.id }) { persona ->
                        PersonaItem(
                            persona = persona,
                            onClick = {
                                onNavigateToDetalle(persona.id, state.asignacionDiariaId ?: "")
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerDesactualizado(fechaAsignacion: String?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Column {
                Text(
                    "Datos no vigentes para hoy",
                    style = MaterialTheme.typography.titleSmall,
                )
                if (fechaAsignacion != null) {
                    Text(
                        "Asignación: $fechaAsignacion",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun BannerSinAsignacion() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Text(
            text = "Sin asignación publicada para hoy",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PersonaItem(
    persona: PersonaResumen,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // weight(1f) reserva primero el ancho natural de la etiqueta de operaciones
        // (medida sin peso) y solo entonces le da el resto al nombre/RUT; sin esto,
        // un nombre de dos líneas mide su línea más larga como ancho "natural" y deja
        // casi nada para la etiqueta, que termina partiéndose carácter por carácter.
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = persona.nombre,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "RUT: ${persona.rutFormateado}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${persona.numOperaciones} op.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false,
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Preview(showBackground = true)
@Composable
private fun PersonaItemNombreCortoPreview() {
    PersonaItem(
        persona = PersonaResumen(
            id = "1",
            nombre = "JUAN PEREZ",
            rutFormateado = "12.345.678-9",
            numOperaciones = 3,
        ),
        onClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PersonaItemNombreLargoPreview() {
    PersonaItem(
        persona = PersonaResumen(
            id = "2",
            nombre = "SARA ANTONIETA DE LOURDES HERNANDEZ SILVA",
            rutFormateado = "12.001.181-2",
            numOperaciones = 12,
        ),
        onClick = {},
    )
}
