package cl.zzenner.cobranza.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

/**
 * Pantalla principal — coordinada por :app.
 * Accede tanto al estado de autenticación (SessionRepository) como
 * al scheduler de asignaciones (AsignacionSyncScheduler) a través de HomeViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onIrAAsignacion: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsState()
    val nombreUsuario = (authState as? AuthState.Autenticado)?.nombreUsuario ?: ""

    // Programar sync al entrar a Home
    LaunchedEffect(Unit) {
        viewModel.iniciarSincronizacion()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cobranza") },
                actions = {
                    TextButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
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

            DisabledFeatureCard(
                titulo = "Registrar gestión",
                descripcion = "Disponible en próxima versión",
            )
        }
    }
}

@Composable
private fun DisabledFeatureCard(titulo: String, descripcion: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }
    }
}
