package cl.zzenner.cobranza.feature.gestion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.zzenner.cobranza.feature.gestion.domain.EstadoSincronizacion
import cl.zzenner.cobranza.feature.gestion.domain.GestionResumen
import cl.zzenner.cobranza.feature.gestion.domain.TipoGestion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionHistorialScreen(
    onNavigateBack: () -> Unit,
    viewModel: GestionHistorialViewModel = hiltViewModel(),
) {
    val gestiones by viewModel.gestiones.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de gestiones") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        if (gestiones.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Sin gestiones registradas", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { }
                items(gestiones, key = { it.id }) { gestion ->
                    GestionItem(gestion)
                }
                item { }
            }
        }
    }
}

@Composable
private fun GestionItem(gestion: GestionResumen) {
    val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(gestion.tipoGestion.etiqueta(), style = MaterialTheme.typography.titleSmall)
                BadgeEstado(gestion.estadoSincronizacion)
            }
            Text(
                formato.format(Date(gestion.fechaGestionEpoch)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            gestion.observacion?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            gestion.fechaCompromiso?.let {
                Text("Compromiso: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BadgeEstado(estado: EstadoSincronizacion) {
    val (etiqueta, color) = when (estado) {
        EstadoSincronizacion.SINCRONIZADA ->
            "Sincronizada" to MaterialTheme.colorScheme.primary
        EstadoSincronizacion.PENDIENTE_ENVIO ->
            "Pendiente" to MaterialTheme.colorScheme.secondary
        EstadoSincronizacion.ENVIANDO ->
            "Enviando" to MaterialTheme.colorScheme.secondary
        EstadoSincronizacion.ERROR_REINTENTABLE ->
            "Error (reintento)" to MaterialTheme.colorScheme.error
        EstadoSincronizacion.ERROR_PERMANENTE ->
            "Error permanente" to MaterialTheme.colorScheme.error
        EstadoSincronizacion.CONFLICTO ->
            "Conflicto" to MaterialTheme.colorScheme.error
    }
    Badge(containerColor = color) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall)
    }
}

private fun TipoGestion.etiqueta(): String = when (this) {
    TipoGestion.SIN_CONTACTO -> "Sin contacto"
    TipoGestion.CONTACTO_FAMILIAR -> "Contacto familiar"
    TipoGestion.COMPROMISO_PAGO -> "Compromiso pago"
}
