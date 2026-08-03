package cl.zzenner.cobranza.feature.busqueda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusquedaDirectaScreen(
    onRegistrarGestion: (personaId: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: BusquedaDirectaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.personaEncontradaId) {
        state.personaEncontradaId?.let { personaId ->
            viewModel.limpiarNavegacion()
            onRegistrarGestion(personaId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar persona por RUT") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Ingrese el RUT de la persona a buscar",
                style = MaterialTheme.typography.bodyLarge,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.rutNumero,
                    onValueChange = viewModel::onRutNumeroChanged,
                    label = { Text("Número") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = state.rutInvalido,
                    modifier = Modifier.weight(1f),
                )
                Text("-", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = state.rutDv,
                    onValueChange = viewModel::onRutDvChanged,
                    label = { Text("DV") },
                    singleLine = true,
                    isError = state.rutInvalido,
                    modifier = Modifier.width(72.dp),
                )
            }

            if (state.rutInvalido) {
                Text(
                    text = "El RUT ingresado no es válido. Verifique el número y dígito verificador.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (state.noEncontrada) {
                Text(
                    text = "No se encontró ninguna persona con el RUT ingresado.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            state.error?.let { mensaje ->
                Text(
                    text = mensaje,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = viewModel::buscar,
                enabled = !state.buscando && state.rutNumero.isNotBlank() && state.rutDv.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.buscando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Buscar")
                }
            }
        }
    }
}
