package cl.zzenner.cobranza.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.zzenner.cobranza.feature.auth.domain.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onIrAAsignacion: () -> Unit,
    onIrABusqueda: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsState()
    val estadoLogout by viewModel.estadoLogout.collectAsState()
    val nombreUsuario = (authState as? AuthState.Autenticado)?.nombreUsuario ?: ""

    LaunchedEffect(Unit) {
        viewModel.iniciarSincronizacion()
    }

    LaunchedEffect(estadoLogout) {
        if (estadoLogout is EstadoLogout.Inactivo && authState is AuthState.NoAutenticado) {
            onLogout()
        }
    }

    when (val logout = estadoLogout) {
        is EstadoLogout.GestionesPendientes -> {
            AlertDialog(
                onDismissRequest = viewModel::cancelarLogout,
                title = { Text("Gestiones sin sincronizar") },
                text = {
                    Text(
                        "Tiene ${logout.cantidad} gestión(es) que aún no han sido enviadas al servidor. " +
                            "¿Desea intentar sincronizarlas antes de cerrar sesión?",
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::sincronizarYLogout) {
                        Text("Sincronizar y cerrar sesión")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelarLogout) {
                        Text("Cancelar")
                    }
                },
            )
        }

        is EstadoLogout.SincronizandoParaCerrar -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Sincronizando...") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator()
                        Text("Enviando gestiones pendientes al servidor")
                    }
                },
                confirmButton = {},
            )
        }

        is EstadoLogout.ErrorSincronizacion -> {
            AlertDialog(
                onDismissRequest = viewModel::cancelarLogout,
                title = { Text("No se pudo sincronizar") },
                text = {
                    Text(
                        "Quedan ${logout.pendientes} gestión(es) sin sincronizar. " +
                            "Verifique la conexión e intente nuevamente.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::sincronizarYLogout) {
                        Text("Reintentar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelarLogout) {
                        Text("Cancelar")
                    }
                },
            )
        }

        else -> {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cobranza") },
                actions = {
                    TextButton(
                        onClick = viewModel::solicitarLogout,
                        enabled = estadoLogout is EstadoLogout.Inactivo,
                    ) {
                        Text("Cerrar sesión")
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
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "Bienvenido, $nombreUsuario",
                style = MaterialTheme.typography.titleLarge,
            )

            HorizontalDivider()

            Button(
                onClick = onIrAAsignacion,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Mi asignación diaria")
            }

            Button(
                onClick = onIrABusqueda,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Buscar persona por RUT")
            }
        }
    }
}
